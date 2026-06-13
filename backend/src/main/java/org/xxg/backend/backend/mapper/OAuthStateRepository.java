package org.xxg.backend.backend.mapper;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.xxg.backend.backend.entity.OAuthState;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * OAuth state 数据访问接口
 */
public interface OAuthStateRepository extends JpaRepository<OAuthState, Integer> {

    /**
     * 根据 state 值查询未过期的记录
     * @param state state nonce
     * @param now 当前时间，用于判断过期
     * @return 匹配的未过期记录
     */
    Optional<OAuthState> findByStateAndExpireTimeAfter(String state, LocalDateTime now);

    /**
     * 根据 state 值删除记录（一次性使用后删除）
     * @param state state nonce
     */
    void deleteByState(String state);

    /**
     * 删除所有过期的 state 记录
     * @param time 过期截止时间
     */
    @Modifying
    @Query("DELETE FROM OAuthState o WHERE o.expireTime < :time")
    void deleteByExpireTimeBefore(@Param("time") LocalDateTime time);
}
