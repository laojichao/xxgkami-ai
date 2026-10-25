package org.xxg.backend.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.xxg.backend.backend.exception.BusinessException;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

/**
 * 数据库备份服务
 * 提供MySQL数据库的备份功能，支持定时备份和手动备份
 * 备份文件存储在用户主目录下的backups文件夹中
 */
@Service
public class BackupService {
    @Value("${spring.datasource.url:}")
    private String dbUrl;
    @Value("${spring.datasource.username:root}")
    private String dbUser;
    @Value("${spring.datasource.password:}")
    private String dbPass;
    /**
     * 备份文件存放目录。
     * <p>默认 {@code ./backups}（相对于应用工作目录，容器内即 /app/backups），
     * 与 docker-compose.yml 中 {@code backup_data:/app/backups} 卷挂载点保持一致，
     * 确保容器重建后备份文件仍然保留。</p>
     * <p>历史缺陷：默认值为 {@code ${user.home}/backups}（容器内为
     * /home/appuser/backups），与卷挂载点不一致，备份既不会落入持久化卷，
     * 且把 backup.dir 显式配置为 /app/backups 时又会被下方路径校验拒绝。</p>
     */
    @Value("${backup.dir:backups}")
    private String backupDir;

    public String backup() throws Exception {
        // 路径遍历验证：确保备份目录在允许的范围内
        Path backupPath = Paths.get(backupDir).toAbsolutePath().normalize();
        Path userHome = Paths.get(System.getProperty("user.home")).toAbsolutePath().normalize();
        Path varBackups = Paths.get("/var/backups").toAbsolutePath().normalize();
        // 应用工作目录（容器内为 /app，对应 compose 的 backup_data 卷挂载点）
        Path appDir = Paths.get("").toAbsolutePath().normalize();
        if (!backupPath.startsWith(userHome)
                && !backupPath.startsWith(varBackups)
                && !backupPath.startsWith(appDir)) {
            throw new BusinessException("备份目录路径不合法");
        }

        // 文件名含毫秒+短随机后缀：此前秒级时间戳下，管理员双击触发两次备份
        // 时两个 mysqldump 进程以截断方式写同一文件，产出交错损坏的 SQL
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
        String filename = "backup_" + timestamp + "_" + Integer.toHexString(new java.security.SecureRandom().nextInt(0xFFFF)) + ".sql";
        new File(backupDir).mkdirs();

        // Validate dbUser to prevent command injection
        if (dbUser == null || !dbUser.matches("^[a-zA-Z0-9_\\-\\.]+$")) {
            throw new Exception("数据库用户名格式无效");
        }

        String dbName = extractDbName(dbUrl);
        // Validate dbName to prevent command injection via JDBC URL manipulation
        if (dbName == null || !dbName.matches("^[a-zA-Z0-9_\\-\\.]+$")) {
            throw new Exception("从数据库 URL 中提取的数据库名称无效: " + dbName);
        }
        // 必须显式传 --host/--port：mysqldump 默认连 localhost:3306，而
        // Docker 部署时 DB_URL 指向 db 容器（compose: jdbc:mysql://db:3306/kami），
        // 缺 --host 会让 /backup 在容器环境下必然失败
        String dbHost = extractDbHost(dbUrl);
        String dbPort = extractDbPort(dbUrl);
        java.util.List<String> cmd = new java.util.ArrayList<>();
        cmd.add("mysqldump");
        cmd.add("--user=" + dbUser);
        cmd.add("--host=" + dbHost);
        cmd.add("--port=" + dbPort);
        cmd.add(dbName);
        ProcessBuilder pb = new ProcessBuilder(cmd);
        if (!dbPass.isEmpty()) pb.environment().put("MYSQL_PWD", dbPass);
        File backupFile = new File(backupDir + "/" + filename);
        pb.redirectOutput(backupFile);
        // 错误输出重定向到单独文件，避免与备份数据混合导致 SQL 文件损坏
        pb.redirectError(new File(backupDir + "/" + filename + ".err"));
        Process process = pb.start();
        try {
            // 超时保护：远程数据库网络抖动/锁等待导致 mysqldump 挂起时，
            // 无限期的 waitFor 会永久占用请求线程，多次触发可耗尽线程池
            if (!process.waitFor(10, java.util.concurrent.TimeUnit.MINUTES)) {
                process.destroyForcibly();
                throw new BusinessException("数据库备份超时（10分钟），已终止备份进程");
            }
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                // 删除失败的半成品 .sql：空/截断文件与成功备份同目录同命名规则，
                // 按文件名匹配的恢复脚本会误取（.err 保留供排查）
                try {
                    java.nio.file.Files.deleteIfExists(backupFile.toPath());
                } catch (Exception ignore) {
                    // 删除失败不影响错误上报
                }
                // BusinessException 而非裸 Exception：走兜底 handler 会变 500
                // "服务器内部错误"，退出码等诊断信息对管理员不可见
                throw new BusinessException("数据库备份失败，退出码: " + exitCode + "（详见同目录 .err 文件）");
            }
        } finally {
            process.destroy();
        }
        // 设置备份文件权限为仅所有者可读写（0600），防止其他用户读取敏感数据
        try {
            Path backupPath2 = backupFile.toPath();
            if (backupPath2.getFileSystem().supportedFileAttributeViews().contains("posix")) {
                Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rw-------");
                Files.setPosixFilePermissions(backupPath2, perms);
            }
        } catch (Exception e) {
            // Windows 等不支持 POSIX 权限的系统忽略此错误
        }
        // 清理空的 .err 文件：备份成功时 stderr 重定向文件通常为空，
        // 残留会在备份卷中随时间累积（仅保留失败场景的 stderr 供排查）
        try {
            File errFile = new File(backupDir + "/" + filename + ".err");
            if (errFile.exists() && errFile.length() == 0) {
                //noinspection ResultOfMethodCallIgnored
                errFile.delete();
            }
        } catch (Exception ignore) {
            // 清理失败不影响备份结果
        }
        // 仅返回文件名，不暴露服务器完整路径
        return filename;
    }

    /**
     * 从 JDBC URL 中提取数据库名。
     * <p>使用正则表达式匹配，兼容 MySQL/PostgreSQL 等 URL 格式，
     * 支持 jdbc:mysql://host:port/dbname?params 和 jdbc:mysql://host/dbname 等变体。</p>
     * @param url JDBC 连接 URL
     * @return 数据库名，解析失败返回空字符串
     */
    private String extractDbName(String url) {
        if (url == null || url.isBlank()) return "";
        // 安全修复：使用正则表达式提取数据库名，兼容各种 URL 格式
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("/([^/?]+)(?:\\?|$)").matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    /**
     * 从 JDBC URL 提取数据库主机，解析失败回退 localhost。
     * <p>仅提取主机名（[a-zA-Z0-9.\-] 字符集）供 ProcessBuilder 参数使用，
     * 防止命令注入；mysqldump 参数以 --host=value 形式传递，不经过 shell。</p>
     */
    private String extractDbHost(String url) {
        if (url == null) return "localhost";
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("jdbc:mysql://([a-zA-Z0-9.\\-]+)").matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "localhost";
    }

    /**
     * 从 JDBC URL 提取数据库端口，未显式指定时回退 MySQL 默认 3306。
     */
    private String extractDbPort(String url) {
        if (url == null) return "3306";
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("jdbc:mysql://[a-zA-Z0-9.\\-]+:(\\d+)").matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "3306";
    }
}
