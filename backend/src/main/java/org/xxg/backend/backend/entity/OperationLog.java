package org.xxg.backend.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 操作日志实体 - 记录管理员在后台的所有操作行为
 */
@Data
@Entity
@Table(name = "operation_logs")
public class OperationLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "admin_id", nullable = false)
    private Integer adminId; // 操作管理员ID

    @Column(name = "admin_username", nullable = false, length = 50)
    private String adminUsername; // 操作管理员用户名

    @Column(name = "operation_type", nullable = false, length = 30)
    private String operationType; // 操作类型(如: create_card, delete_user等)

    @Column(name = "operation_content", nullable = false, columnDefinition = "TEXT")
    private String operationContent; // 操作详细内容描述

    @Column(name = "ip_address", length = 50)
    private String ipAddress; // 操作者IP地址

    @Column(name = "create_time")
    private LocalDateTime createTime = LocalDateTime.now();
}
