package org.xxg.backend.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * API密钥机器码兑换记录 - 记录API密钥与机器码的一次性绑定关系
 */
@Data
@EqualsAndHashCode(of = "id")
@Entity
@Table(name = "api_key_machine_spec_redemption")
public class ApiKeyMachineSpecRedemption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "api_key_id", nullable = false)
    private Long apiKeyId; // 关联的API密钥ID

    @Column(name = "machine_code", nullable = false, length = 255)
    private String machineCode; // 客户端机器码

    @Column(name = "spec_key", nullable = false, length = 128)
    private String specKey; // 规格标识(用于区分不同兑换规格)

    @Column(name = "create_time")
    private LocalDateTime createTime = LocalDateTime.now();
}
