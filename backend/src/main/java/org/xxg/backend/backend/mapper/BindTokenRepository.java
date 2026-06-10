package org.xxg.backend.backend.mapper;

import org.springframework.data.jpa.repository.JpaRepository;
import org.xxg.backend.backend.entity.BindToken;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 绑定令牌数据访问接口
 */
public interface BindTokenRepository extends JpaRepository<BindToken, Integer> {

    /**
     * 根据令牌内容查询未使用的令牌
     * @param token 令牌内容
     * @return 匹配的未使用令牌
     */
    Optional<BindToken> findByTokenAndUsedFalse(String token);

    /**
     * 删除指定时间之前的过期令牌，用于定期清理
     * @param time 过期截止时间
     */
    void deleteByExpireTimeBefore(LocalDateTime time);
}
