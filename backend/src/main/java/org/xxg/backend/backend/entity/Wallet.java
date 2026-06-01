package org.xxg.backend.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 钱包实体 - 用户账户余额及资金统计
 */
@Data
@Entity
@Table(name = "wallets")
public class Wallet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Integer userId; // 关联的用户ID(一对一)

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO; // 当前可用余额(元)

    @Column(name = "total_recharge", precision = 10, scale = 2)
    private BigDecimal totalRecharge = BigDecimal.ZERO; // 累计充值金额(元)

    @Column(name = "total_consume", precision = 10, scale = 2)
    private BigDecimal totalConsume = BigDecimal.ZERO; // 累计消费金额(元)

    @Column(name = "create_time")
    private LocalDateTime createTime = LocalDateTime.now();

    @Column(name = "update_time")
    private LocalDateTime updateTime = LocalDateTime.now();
}
