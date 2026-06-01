package org.xxg.backend.backend.mapper;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.xxg.backend.backend.entity.OperationLog;
import java.time.LocalDateTime;

/**
 * 操作日志数据访问接口
 * 提供操作日志的增删改查及按管理员、操作类型、时间范围分页查询功能
 */
public interface OperationLogRepository extends JpaRepository<OperationLog, Integer> {
    /** 根据管理员ID分页查询操作日志 */
    Page<OperationLog> findByAdminId(Integer adminId, Pageable pageable);
    /** 根据操作类型分页查询操作日志 */
    Page<OperationLog> findByOperationType(String operationType, Pageable pageable);
    /** 根据创建时间范围分页查询操作日志 */
    Page<OperationLog> findByCreateTimeBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);
}
