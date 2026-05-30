package org.xxg.backend.backend.mapper;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.xxg.backend.backend.entity.Order;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Integer> {
    Optional<Order> findByOrderNo(String orderNo);
    List<Order> findByUserId(Integer userId);
    Page<Order> findByUserId(Integer userId, Pageable pageable);
    List<Order> findByStatus(String status);
    long countByStatus(String status);
    long countByCreateTimeAfter(LocalDateTime time);
    Page<Order> findAll(Pageable pageable);
}
