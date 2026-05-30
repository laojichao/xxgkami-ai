package org.xxg.backend.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "card_cipher")
public class CardCipher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "card_hash", nullable = false, unique = true, length = 255)
    private String cardHash;

    @Column(name = "cipher_data", nullable = false, columnDefinition = "TEXT")
    private String cipherData;

    @Column(name = "sign_data", nullable = false, columnDefinition = "TEXT")
    private String signData;

    @Column(nullable = false, length = 64)
    private String salt;

    @Column(nullable = false, length = 64)
    private String iv;

    @Column(name = "create_time")
    private LocalDateTime createTime = LocalDateTime.now();
}
