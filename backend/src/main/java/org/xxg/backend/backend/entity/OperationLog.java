package org.xxg.backend.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "operation_logs")
public class OperationLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "admin_id", nullable = false)
    private Integer adminId;

    @Column(name = "admin_username", nullable = false, length = 50)
    private String adminUsername;

    @Column(name = "operation_type", nullable = false, length = 30)
    private String operationType;

    @Column(name = "operation_content", nullable = false, columnDefinition = "TEXT")
    private String operationContent;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "create_time")
    private LocalDateTime createTime = LocalDateTime.now();
}
