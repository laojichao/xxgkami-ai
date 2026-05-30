package org.xxg.backend.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "api_keys")
public class ApiKey {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "key_name", nullable = false, length = 50)
    private String keyName;

    @Column(name = "api_key", nullable = false, unique = true, length = 32)
    private String apiKeyValue;

    private Boolean status = true;

    @Column(name = "create_time")
    private LocalDateTime createTime = LocalDateTime.now();

    @Column(name = "last_use_time")
    private LocalDateTime lastUseTime;

    @Column(name = "use_count")
    private Integer useCount = 0;

    @Column(length = 255)
    private String description;

    @Column(name = "key_value", nullable = false, unique = true, length = 255)
    private String keyValue;

    @Column(nullable = false, length = 100)
    private String name = "API Key";

    @Column(name = "update_time")
    private LocalDateTime updateTime = LocalDateTime.now();

    @Column(name = "webhook_config", columnDefinition = "TEXT")
    private String webhookConfig;

    @Column(name = "enable_card_encryption")
    private Boolean enableCardEncryption = false;

    @Column(name = "require_machine_code")
    private Boolean requireMachineCode = false;

    @Column(name = "machine_spec_once_config", columnDefinition = "TEXT")
    private String machineSpecOnceConfig;
}
