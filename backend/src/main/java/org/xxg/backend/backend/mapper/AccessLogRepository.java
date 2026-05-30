package org.xxg.backend.backend.mapper;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.xxg.backend.backend.entity.AccessLog;
import java.time.LocalDateTime;

public interface AccessLogRepository extends JpaRepository<AccessLog, Long> {
    Page<AccessLog> findByIp(String ip, Pageable pageable);
    Page<AccessLog> findByUsername(String username, Pageable pageable);
    Page<AccessLog> findByCreateTimeBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);
    long countByCreateTimeAfter(LocalDateTime time);
}
