package org.xxg.backend.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统特性/功能展示实体 - 首页展示的系统功能亮点
 */
@Data
@EqualsAndHashCode(of = "id")
@Entity
@Table(name = "features")
public class Feature {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 50)
    private String icon; // 图标标识(如图标类名或图标名称)

    @Column(nullable = false, length = 100)
    private String title; // 功能标题

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description; // 功能描述

    @Column(name = "sort_order")
    private Integer sortOrder = 0; // 排序权重(越小越靠前)

    private Boolean status = true; // 是否启用展示
}
