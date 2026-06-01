package org.xxg.backend.backend.mapper;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.xxg.backend.backend.entity.VerificationCode;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 验证码数据访问接口
 * 提供验证码的增删改查、最新验证码查询、过期清理及频率校验功能
 */
public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Integer> {
    /** 查询指定邮箱和类型的最新一条验证码 */
    Optional<VerificationCode> findTopByEmailAndTypeOrderByCreateTimeDesc(String email, String type);
    /** 删除所有已过期的验证码 */
    @Modifying
    @Query("DELETE FROM VerificationCode v WHERE v.expireTime < :now")
    void deleteExpiredCodes(@Param("now") LocalDateTime now);

    /** 判断指定邮箱和类型在指定时间之后是否存在验证码（用于发送频率限制） */
    @Query("SELECT COUNT(v) > 0 FROM VerificationCode v WHERE v.email = :email AND v.type = :type AND v.createTime > :since")
    boolean existsRecentCode(@Param("email") String email, @Param("type") String type, @Param("since") LocalDateTime since);
}
