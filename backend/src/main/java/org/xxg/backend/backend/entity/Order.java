package org.xxg.backend.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.Objects;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体 - 用户购买卡密的订单记录
 */
@Getter
@Setter
@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_order_user_id", columnList = "user_id"),
    @Index(name = "idx_order_status", columnList = "status"),
    @Index(name = "idx_order_create_time", columnList = "create_time")
})
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "order_no", nullable = false, unique = true, length = 32)
    private String orderNo; // 订单编号(唯一标识)

    @Column(name = "user_id", nullable = false)
    private Integer userId; // 下单用户ID

    @Column(length = 50)
    private String username; // 下单用户名(冗余存储便于查询)

    @Column(name = "card_type", nullable = false, length = 20)
    private String cardType; // 卡密类型(time=时长卡, count=次数卡)

    @Column(name = "card_spec", nullable = false, length = 50)
    private String cardSpec; // 卡密规格(如"7天", "100次")

    private Integer quantity = 1; // 购买数量

    @Column(name = "unit_price", precision = 10, scale = 2)
    private BigDecimal unitPrice = BigDecimal.ZERO; // 单价(元)

    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice; // 总价(元)

    @Column(nullable = false, length = 20)
    private String status = "pending"; // 订单状态: pending=待支付, completed=已完成, failed=已失败, cancelled=已取消
    // 注意：status 保持 String 类型以兼容数据库现有数据，业务代码应参考 OrderStatus 枚举进行状态比较

    @Column(name = "payment_method", length = 20)
    private String paymentMethod = "wechat"; // 支付方式: wechat=微信, alipay=支付宝

    @Column(name = "create_time")
    private LocalDateTime createTime = LocalDateTime.now();

    @Column(name = "update_time")
    private LocalDateTime updateTime; // 订单更新时间

    @Column(name = "pay_time")
    private LocalDateTime payTime; // 支付完成时间

    @JsonIgnore
    @Column(name = "card_keys", columnDefinition = "TEXT")
    private String cardKeys; // 购买的卡密内容(支付成功后填充, JSON格式)

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order that = (Order) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
