package org.xxg.backend.backend.mapper;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.xxg.backend.backend.entity.IpBlacklist;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * IP黑名单数据访问接口
 * 提供IP黑名单的增删改查、封禁状态判断及过期记录清理功能
 */
public interface IpBlacklistRepository extends JpaRepository<IpBlacklist, Long> {
    /** 根据IP地址查询黑名单记录 */
    Optional<IpBlacklist> findByIpAddress(String ipAddress);

    /** 判断指定IP是否处于封禁状态（永久封禁或未过期的临时封禁） */
    @Query("SELECT COUNT(i) > 0 FROM IpBlacklist i WHERE i.ipAddress = :ip " +
           "AND (i.permanent = true OR i.blockedUntil > :now)")
    boolean isBlocked(@Param("ip") String ip, @Param("now") LocalDateTime now);

    /** 删除已过期的临时封禁记录 */
    @Modifying
    @Query("DELETE FROM IpBlacklist i WHERE i.permanent = false AND i.blockedUntil < :now")
    void deleteExpired(@Param("now") LocalDateTime now);

    /** 根据IP地址删除黑名单记录 */
    void deleteByIpAddress(String ipAddress);
}
