package org.xxg.backend.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "social_users")
public class SocialUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "social_uid", nullable = false, length = 100)
    private String socialUid;

    @Column(name = "social_type", nullable = false, length = 20)
    private String socialType;

    @Column(name = "create_time")
    private LocalDateTime createTime = LocalDateTime.now();
}
