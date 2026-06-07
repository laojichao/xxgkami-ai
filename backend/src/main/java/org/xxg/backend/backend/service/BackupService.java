package org.xxg.backend.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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

    public String backup() throws Exception {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = "backup_" + timestamp + ".sql";
        String backupDir = System.getProperty("user.home") + "/backups";
        new File(backupDir).mkdirs();

        // Validate dbUser to prevent command injection
        if (dbUser == null || !dbUser.matches("^[a-zA-Z0-9_\\-\\.]+$")) {
            throw new Exception("Invalid database username");
        }

        String dbName = extractDbName(dbUrl);
        ProcessBuilder pb = new ProcessBuilder(
            "mysqldump", "--user=" + dbUser, dbName);
        if (!dbPass.isEmpty()) pb.environment().put("MYSQL_PWD", dbPass);
        pb.redirectOutput(new File(backupDir + "/" + filename));
        pb.redirectErrorStream(true);
        Process process = pb.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) throw new Exception("Backup failed with exit code: " + exitCode);
        return backupDir + "/" + filename;
    }

    private String extractDbName(String url) {
        String[] parts = url.split("/");
        String last = parts[parts.length - 1];
        return last.contains("?") ? last.substring(0, last.indexOf("?")) : last;
    }
}
