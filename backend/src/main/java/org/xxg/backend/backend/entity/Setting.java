package org.xxg.backend.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * 系统设置实体 - 键值对形式存储系统配置项
 */
@Data
@Entity
@Table(name = "settings")
public class Setting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 50)
    private String name; // 配置项名称(唯一键)

    @Column(nullable = false, columnDefinition = "TEXT")
    private String value; // 配置项值(JSON或纯文本)
}
