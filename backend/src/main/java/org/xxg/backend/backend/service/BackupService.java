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
    @Value("${backup.dir:${user.home}/backups}")
    private String backupDir;

    public String backup() throws Exception {
        // 路径遍历验证：确保备份目录在允许的范围内
        Path backupPath = Paths.get(backupDir).toAbsolutePath().normalize();
        Path userHome = Paths.get(System.getProperty("user.home")).toAbsolutePath().normalize();
        Path varBackups = Paths.get("/var/backups").toAbsolutePath().normalize();
        if (!backupPath.startsWith(userHome) && !backupPath.startsWith(varBackups)) {
            throw new BusinessException("备份目录路径不合法");
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = "backup_" + timestamp + ".sql";
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
        ProcessBuilder pb = new ProcessBuilder(
            "mysqldump", "--user=" + dbUser, dbName);
        if (!dbPass.isEmpty()) pb.environment().put("MYSQL_PWD", dbPass);
        File backupFile = new File(backupDir + "/" + filename);
        pb.redirectOutput(backupFile);
        // 错误输出重定向到单独文件，避免与备份数据混合导致 SQL 文件损坏
        pb.redirectError(new File(backupDir + "/" + filename + ".err"));
        Process process = pb.start();
        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) throw new Exception("数据库备份失败，退出码: " + exitCode);
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
        // 仅返回文件名，不暴露服务器完整路径
        return filename;
    }

    private String extractDbName(String url) {
        String[] parts = url.split("/");
        String last = parts[parts.length - 1];
        return last.contains("?") ? last.substring(0, last.indexOf("?")) : last;
    }
}
