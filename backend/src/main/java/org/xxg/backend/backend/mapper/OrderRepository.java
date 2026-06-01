package org.xxg.backend.backend.mapper;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.xxg.backend.backend.entity.Order;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 订单数据访问接口
 * 提供订单的增删改查、按订单号/用户/状态查询以及订单统计功能
 */
public interface OrderRepository extends JpaRepository<Order, Integer> {
    /** 根据订单号查询订单 */
    Optional<Order> findByOrderNo(String orderNo);
    /** 根据用户ID查询订单列表 */
    List<Order> findByUserId(Integer userId);
    /** 根据用户ID分页查询订单 */
    Page<Order> findByUserId(Integer userId, Pageable pageable);
    /** 根据订单状态查询订单列表 */
    List<Order> findByStatus(String status);
    /** 统计指定状态的订单数量 */
    long countByStatus(String status);
    /** 统计指定时间之后创建的订单数量 */
    long countByCreateTimeAfter(LocalDateTime time);
    /** 分页查询所有订单 */
    Page<Order> findAll(Pageable pageable);
}
