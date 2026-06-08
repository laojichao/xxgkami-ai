package org.xxg.backend.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

import org.hibernate.annotations.UpdateTimestamp;

/**
 * API密钥实体 - 管理第三方接口调用的密钥信息
 */
@Data
@Entity
@Table(name = "api_keys")
public class ApiKey {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "key_name", nullable = false, length = 50)
    private String keyName; // 密钥显示名称

    @Column(name = "api_key", nullable = false, unique = true, length = 32)
    private String apiKeyValue; // API密钥值(用于接口认证)

    private Boolean status = true; // 密钥状态(true=启用, false=禁用)

    @Column(name = "create_time")
    private LocalDateTime createTime = LocalDateTime.now();

    @Column(name = "last_use_time")
    private LocalDateTime lastUseTime; // 最后使用时间

    @Column(name = "use_count")
    private Integer useCount = 0; // 累计使用次数

    @Column(length = 255)
    private String description; // 密钥用途描述

    @Column(name = "key_value", nullable = false, unique = true, length = 255)
    private String keyValue; // 密钥完整值(内部存储)

    @Column(nullable = false, length = 100)
    private String name = "API Key"; // 密钥别名

    @Column(name = "update_time")
    @UpdateTimestamp
    private LocalDateTime updateTime;

    @Column(name = "webhook_config", columnDefinition = "TEXT")
    private String webhookConfig; // Webhook回调配置(JSON格式)

    @Column(name = "enable_card_encryption")
    private Boolean enableCardEncryption = false; // 是否启用卡密加密传输

    @Column(name = "require_machine_code")
    private Boolean requireMachineCode = false; // 是否要求绑定机器码

    @Column(name = "machine_spec_once_config", columnDefinition = "TEXT")
    private String machineSpecOnceConfig; // 机器码一次性绑定配置(JSON格式)
}
