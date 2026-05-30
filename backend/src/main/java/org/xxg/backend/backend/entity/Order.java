package org.xxg.backend.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "order_no", nullable = false, unique = true, length = 32)
    private String orderNo;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(length = 50)
    private String username;

    @Column(name = "card_type", nullable = false, length = 20)
    private String cardType;

    @Column(name = "card_spec", nullable = false, length = 50)
    private String cardSpec;

    private Integer quantity = 1;

    @Column(name = "unit_price", precision = 10, scale = 2)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Column(nullable = false, length = 20)
    private String status = "pending";

    @Column(name = "payment_method", length = 20)
    private String paymentMethod = "wechat";

    @Column(name = "create_time")
    private LocalDateTime createTime = LocalDateTime.now();

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @Column(name = "pay_time")
    private LocalDateTime payTime;

    @Column(name = "card_keys", columnDefinition = "TEXT")
    private String cardKeys;
}
