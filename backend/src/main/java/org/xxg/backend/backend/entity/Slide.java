package org.xxg.backend.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 轮播图实体 - 首页轮播图/横幅广告管理
 */
@Data
@Entity
@Table(name = "slides")
public class Slide {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String title; // 轮播图标题

    @Column(nullable = false, length = 255)
    private String description; // 轮播图描述文字

    @Column(name = "image_url", nullable = false, length = 255)
    private String imageUrl; // 轮播图片URL地址

    @Column(name = "sort_order")
    private Integer sortOrder = 0; // 排序权重(越小越靠前)

    private Boolean status = true; // 是否启用展示

    @Column(name = "create_time")
    private LocalDateTime createTime = LocalDateTime.now();
}
