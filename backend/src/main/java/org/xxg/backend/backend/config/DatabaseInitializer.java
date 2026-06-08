package org.xxg.backend.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.xxg.backend.backend.entity.MaintenanceSettings;

/**
 * 数据库初始化器
 * <p>应用启动时自动执行，负责：</p>
 * <ul>
 *   <li>验证关键安全配置（JWT_SECRET、DB_PASSWORD）是否存在且符合要求</li>
 *   <li>初始化系统维护设置（如果尚未创建）</li>
 *   <li>确保管理员密码已使用BCrypt加密存储</li>
 * </ul>
 */
@Component
public class DatabaseInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // Validate critical security configs
        if (jwtSecret == null || jwtSecret.isEmpty()) {
            throw new RuntimeException("JWT_SECRET 环境变量未配置，应用无法启动");
        }
        if (jwtSecret.length() < 32) {
            throw new RuntimeException("JWT_SECRET 长度不足 32 字节，当前长度: " + jwtSecret.length());
        }
        if (dbPassword == null || dbPassword.isEmpty()) {
            throw new RuntimeException("DB_PASSWORD 环境变量未配置，应用无法启动");
        }
        // Initialize maintenance settings if not exists
        try {
            MaintenanceSettings settings = entityManager.find(MaintenanceSettings.class, 1);
            if (settings == null) {
                settings = new MaintenanceSettings();
                settings.setId(1);
                settings.setEnabled(false);
                settings.setContent("系统正在维护中，请稍后访问。");
                settings.setMaintenanceTime("8小时");
                settings.setEmailSubject("小小怪卡密系统维护通知");
                entityManager.persist(settings);
            }
        } catch (Exception e) {
            log.debug("维护设置初始化跳过（表可能尚不存在）: {}", e.getMessage());
        }

        // Ensure admin password is BCrypt encrypted
        try {
            var query = entityManager.createNativeQuery(
                "SELECT password FROM admins WHERE username = 'admin'");
            Object result = query.getSingleResult();
            if (result != null) {
                String password = result.toString();
                if (!password.startsWith("$2a$") && !password.startsWith("$2b$") && !password.startsWith("$2y$")) {
                    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
                    String encoded = encoder.encode(password);
                    entityManager.createNativeQuery(
                        "UPDATE admins SET password = :pwd WHERE username = 'admin'")
                        .setParameter("pwd", encoded)
                        .executeUpdate();
                }
            }
        } catch (Exception e) {
            log.debug("管理员密码迁移跳过（管理员可能尚不存在）: {}", e.getMessage());
        }
    }
}
