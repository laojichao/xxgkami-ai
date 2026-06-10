package org.xxg.backend.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.UpdateTimestamp;

/**
 * 卡密定价实体 - 定义不同类型和规格卡密的价格
 */
@Data
@EqualsAndHashCode(of = "id")
@Entity
@Table(name = "card_pricing")
public class CardPricing {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 20)
    @NotBlank(message = "卡密类型不能为空")
    @Size(max = 50)
    private String type; // 卡密类型: time=时长卡, count=次数卡

    @Column(nullable = false)
    @NotNull(message = "规格值不能为空")
    private Integer value; // 规格值(时长卡=天数, 次数卡=次数)

    @Column(nullable = false, precision = 10, scale = 2)
    @NotNull(message = "价格不能为空")
    @Positive(message = "价格必须大于0")
    private BigDecimal price; // 价格(元)

    @Column(nullable = false, length = 100)
    @NotBlank(message = "描述不能为空")
    @Size(max = 500)
    private String description; // 规格描述(如"7天卡", "100次卡")

    @Column(name = "create_time")
    private LocalDateTime createTime = LocalDateTime.now();

    @Column(name = "update_time")
    @UpdateTimestamp
    private LocalDateTime updateTime;
}
