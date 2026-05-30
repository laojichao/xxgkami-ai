package org.xxg.backend.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "api_key_machine_spec_redemption")
public class ApiKeyMachineSpecRedemption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "api_key_id", nullable = false)
    private Long apiKeyId;

    @Column(name = "machine_code", nullable = false, length = 255)
    private String machineCode;

    @Column(name = "spec_key", nullable = false, length = 128)
    private String specKey;

    @Column(name = "create_time")
    private LocalDateTime createTime = LocalDateTime.now();
}
