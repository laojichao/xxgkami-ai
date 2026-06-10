package org.xxg.backend.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * 卡密加密数据实体 - 存储卡密的加密信息，用于安全验证
 */
@Data
@EqualsAndHashCode(of = "id")
@Entity
@Table(name = "card_cipher")
public class CardCipher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "card_hash", nullable = false, unique = true, length = 255)
    private String cardHash; // 卡密哈希值(用于索引查找)

    @Column(name = "cipher_data", nullable = false, columnDefinition = "TEXT")
    private String cipherData; // 加密后的卡密数据

    @Column(name = "sign_data", nullable = false, columnDefinition = "TEXT")
    private String signData; // 签名数据(用于完整性校验)

    @Column(nullable = false, length = 64)
    private String salt; // 加密盐值

    @Column(nullable = false, length = 64)
    private String iv; // 初始化向量(AES加密使用)

    @Column(name = "create_time")
    private LocalDateTime createTime = LocalDateTime.now();
}
