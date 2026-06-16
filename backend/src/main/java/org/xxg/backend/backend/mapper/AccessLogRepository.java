package org.xxg.backend.backend.mapper;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.xxg.backend.backend.entity.AccessLog;
import java.time.LocalDateTime;

/**
 * 访问日志数据访问接口
 * 提供访问日志的增删改查及按IP、用户名、时间范围的查询统计功能
 */
public interface AccessLogRepository extends JpaRepository<AccessLog, Long> {
    /** 根据IP地址分页查询访问日志 */
    Page<AccessLog> findByIp(String ip, Pageable pageable);
    /** 根据用户名分页查询访问日志 */
    Page<AccessLog> findByUsername(String username, Pageable pageable);
    /** 根据创建时间范围分页查询访问日志 */
    Page<AccessLog> findByCreateTimeBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);
    /** 统计指定时间之后的访问日志数量 */
    long countByCreateTimeAfter(LocalDateTime time);
    /** 删除指定时间之前的访问日志 */
    @Modifying
    @Query("DELETE FROM AccessLog a WHERE a.createTime < :before")
    int deleteByCreateTimeBefore(@Param("before") LocalDateTime before);
}
