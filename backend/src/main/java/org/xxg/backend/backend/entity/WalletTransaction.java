package org.xxg.backend.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 钱包交易记录实体 - 记录用户钱包的所有资金变动明细
 */
@Data
@Entity
@Table(name = "wallet_transactions", indexes = {
    @Index(name = "idx_wtx_user_time", columnList = "user_id, create_time")
})
public class WalletTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Integer userId; // 关联的用户ID

    @Column(nullable = false, length = 20)
    private String type; // 交易类型: recharge=充值, consume=消费, refund=退款

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount; // 交易金额(元, 正数)

    @Column(name = "balance_after", precision = 10, scale = 2)
    private BigDecimal balanceAfter; // 交易后余额快照(元)

    @Column(length = 255)
    private String description; // 交易描述/备注

    @Column(name = "order_no", length = 32)
    private String orderNo; // 关联的订单编号(消费/退款时有值)

    @Column(name = "create_time")
    private LocalDateTime createTime = LocalDateTime.now();
}
