package org.xxg.backend.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "system_maintenance")
public class MaintenanceSettings {
    @Id
    private Integer id = 1;

    private Boolean enabled = false;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "maintenance_time", length = 255)
    private String maintenanceTime;

    @Column(name = "start_time", length = 255)
    private String startTime;

    @Column(name = "email_subject", length = 255)
    private String emailSubject;

    @Column(name = "email_template", columnDefinition = "TEXT")
    private String emailTemplate;
}
