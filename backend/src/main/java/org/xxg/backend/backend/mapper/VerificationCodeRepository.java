package org.xxg.backend.backend.mapper;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.xxg.backend.backend.entity.VerificationCode;
import java.time.LocalDateTime;
import java.util.Optional;

public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Integer> {
    Optional<VerificationCode> findTopByEmailAndTypeOrderByCreateTimeDesc(String email, String type);
    @Modifying
    @Query("DELETE FROM VerificationCode v WHERE v.expireTime < :now")
    void deleteExpiredCodes(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(v) > 0 FROM VerificationCode v WHERE v.email = :email AND v.type = :type AND v.createTime > :since")
    boolean existsRecentCode(@Param("email") String email, @Param("type") String type, @Param("since") LocalDateTime since);
}
