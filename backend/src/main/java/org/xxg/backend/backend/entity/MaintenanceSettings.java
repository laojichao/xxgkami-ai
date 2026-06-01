package org.xxg.backend.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * 系统维护设置实体 - 单例模式，控制系统维护模式的开关和相关配置
 */
@Data
@Entity
@Table(name = "system_maintenance")
public class MaintenanceSettings {
    @Id
    private Integer id = 1; // 固定为1，单例模式

    private Boolean enabled = false; // 是否开启维护模式

    @Column(columnDefinition = "TEXT")
    private String content; // 维护公告内容(HTML格式)

    @Column(name = "maintenance_time", length = 255)
    private String maintenanceTime; // 维护时间说明文本

    @Column(name = "start_time", length = 255)
    private String startTime; // 维护开始时间

    @Column(name = "email_subject", length = 255)
    private String emailSubject; // 维护通知邮件主题

    @Column(name = "email_template", columnDefinition = "TEXT")
    private String emailTemplate; // 维护通知邮件模板(HTML格式)
}
