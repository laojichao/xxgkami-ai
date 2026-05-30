package org.xxg.backend.backend.mapper;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.xxg.backend.backend.entity.OperationLog;
import java.time.LocalDateTime;

public interface OperationLogRepository extends JpaRepository<OperationLog, Integer> {
    Page<OperationLog> findByAdminId(Integer adminId, Pageable pageable);
    Page<OperationLog> findByOperationType(String operationType, Pageable pageable);
    Page<OperationLog> findByCreateTimeBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);
}
