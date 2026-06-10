package org.xxg.backend.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xxg.backend.backend.dto.CreateOrderRequest;
import org.xxg.backend.backend.entity.*;
import org.xxg.backend.backend.exception.BusinessException;
import org.xxg.backend.backend.mapper.*;
import org.xxg.backend.backend.util.PaymentUtil;
import jakarta.persistence.criteria.Predicate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 订单业务服务。
 * <p>提供订单的创建、完成、失败、查询及统计功能，
 * 支持卡密购买场景下的订单全生命周期管理。</p>
 */
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CardPricingRepository cardPricingRepository;
    private final CardService cardService;
    private final PaymentUtil paymentUtil;
    private final EmailService emailService;
    private final UserRepository userRepository;

    public OrderService(OrderRepository orderRepository, CardPricingRepository cardPricingRepository,
                        CardService cardService, PaymentUtil paymentUtil, EmailService emailService,
                        UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.cardPricingRepository = cardPricingRepository;
        this.cardService = cardService;
        this.paymentUtil = paymentUtil;
        this.emailService = emailService;
        this.userRepository = userRepository;
    }

    /**
     * 创建订单。
     * <p>校验用户和价格配置后生成订单号，计算总价，状态为 pending。</p>
     *
     * @param request 创建订单请求 DTO
     * @return 创建的订单实体
     */
    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        // 校验卡密类型是否合法
        if (!"time".equals(request.getCardType()) && !"count".equals(request.getCardType())) {
            throw new BusinessException("无效的卡密类型，仅支持 time 或 count");
        }
        // 校验购买数量范围
        if (request.getQuantity() == null || request.getQuantity() <= 0 || request.getQuantity() > 1000) {
            throw new BusinessException("购买数量必须在 1-1000 之间");
        }
        // 用户ID是必填项，不允许创建匿名订单
        if (request.getUserId() == null) {
            throw new BusinessException("用户ID不能为空");
        }
        userRepository.findById(request.getUserId())
                .orElseThrow(() -> new BusinessException("用户不存在"));

        CardPricing pricing = cardPricingRepository.findById(request.getPricingId())
                .orElseThrow(() -> new BusinessException("价格配置不存在"));

        Order order = new Order();
        order.setOrderNo(paymentUtil.generateOrderNo());
        order.setUserId(request.getUserId());
        order.setUsername(request.getUsername());
        order.setCardType(request.getCardType());
        order.setCardSpec(request.getCardSpec());
        order.setQuantity(request.getQuantity());
        order.setUnitPrice(pricing.getPrice());
        order.setTotalPrice(pricing.getPrice().multiply(BigDecimal.valueOf(request.getQuantity())));
        order.setStatus("pending");
        order.setPaymentMethod(request.getPaymentMethod());
        order.setCreateTime(LocalDateTime.now());

        return orderRepository.save(order);
    }

    /**
     * 完成订单（支付成功后调用）。
     *
     * @param orderNo  订单号
     * @param cardKeys 生成的卡密明文（多个以逗号分隔）
     */
    @Transactional
    public void completeOrder(String orderNo, String cardKeys) {
        Order order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new BusinessException("订单不存在"));
        // 幂等性检查：已完成的订单不重复处理，防止支付回调重复发送导致卡密重复生成
        if ("completed".equals(order.getStatus())) {
            return; // 已完成，直接返回
        }
        if (!"pending".equals(order.getStatus())) {
            throw new BusinessException("订单状态不允许完成: " + order.getStatus());
        }
        order.setStatus("completed");
        order.setPayTime(LocalDateTime.now());
        order.setCardKeys(cardKeys);
        orderRepository.save(order);
    }

    /**
     * 标记订单为失败状态。
     *
     * @param orderNo 订单号
     */
    @Transactional
    public void failOrder(String orderNo) {
        Order order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new BusinessException("订单不存在"));
        // 幂等性检查：已完成的订单不能标记为失败，防止误操作
        if ("completed".equals(order.getStatus())) {
            throw new BusinessException("已完成的订单不能标记为失败");
        }
        order.setStatus("failed");
        order.setUpdateTime(LocalDateTime.now());
        orderRepository.save(order);
    }

    /**
     * 根据订单号查询订单。
     *
     * @param orderNo 订单号
     * @return 订单实体，不存在时返回 null
     */
    @Transactional(readOnly = true)
    public Order getOrderByNo(String orderNo) {
        return orderRepository.findByOrderNo(orderNo).orElse(null);
    }

    /**
     * 查询指定用户的全部订单列表（不分页）。
     *
     * @param userId 用户 ID
     * @return 订单列表
     */
    @Transactional(readOnly = true)
    public List<Order> getOrdersByUserId(Integer userId) {
        return orderRepository.findByUserId(userId);
    }

    /**
     * 分页查询指定用户的订单。
     *
     * @param userId   用户 ID
     * @param pageable 分页参数
     * @return 订单分页结果
     */
    @Transactional(readOnly = true)
    public Page<Order> getOrdersByUserId(Integer userId, Pageable pageable) {
        return orderRepository.findByUserId(userId, pageable);
    }

    /**
     * 分页查询全部订单。
     *
     * @param pageable 分页参数
     * @return 订单分页结果
     */
    @Transactional(readOnly = true)
    public Page<Order> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable);
    }

    /**
     * 按状态分页查询订单。
     *
     * @param status   订单状态
     * @param pageable 分页参数
     * @return 订单分页结果
     */
    @Transactional(readOnly = true)
    public Page<Order> getOrdersByStatus(String status, Pageable pageable) {
        return orderRepository.findByStatus(status, pageable);
    }

    /**
     * 多条件动态查询订单（支持状态、订单号、用户名、时间范围筛选）。
     * <p>使用 JPA Specification 构建动态查询条件，所有参数均可选。</p>
     *
     * @param status    订单状态（可选）
     * @param orderNo   订单号模糊搜索（可选）
     * @param username  用户名模糊搜索（可选）
     * @param startDate 创建时间范围起始，格式 yyyy-MM-dd（可选）
     * @param endDate   创建时间范围结束，格式 yyyy-MM-dd（可选）
     * @param pageable  分页参数
     * @return 订单分页结果
     */
    @Transactional(readOnly = true)
    public Page<Order> searchOrders(String status, String orderNo, String username,
                                     String startDate, String endDate, Pageable pageable) {
        Specification<Order> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (orderNo != null && !orderNo.isBlank()) {
                predicates.add(cb.like(root.get("orderNo"), "%" + orderNo + "%"));
            }
            if (username != null && !username.isBlank()) {
                predicates.add(cb.like(root.get("username"), "%" + username + "%"));
            }
            if (startDate != null && !startDate.isBlank()) {
                LocalDate start = LocalDate.parse(startDate, DateTimeFormatter.ISO_LOCAL_DATE);
                predicates.add(cb.greaterThanOrEqualTo(root.get("createTime"),
                        start.atStartOfDay()));
            }
            if (endDate != null && !endDate.isBlank()) {
                LocalDate end = LocalDate.parse(endDate, DateTimeFormatter.ISO_LOCAL_DATE);
                predicates.add(cb.lessThanOrEqualTo(root.get("createTime"),
                        end.atTime(LocalTime.MAX)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return orderRepository.findAll(spec, pageable);
    }

    /**
     * 获取订单统计信息。
     * <p>包含总数、待支付/已完成/已失败订单数、今日新增订单数。</p>
     *
     * @return 统计数据 Map
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getOrderStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalOrders", orderRepository.count());
        stats.put("pendingOrders", orderRepository.countByStatus("pending"));
        stats.put("completedOrders", orderRepository.countByStatus("completed"));
        stats.put("failedOrders", orderRepository.countByStatus("failed"));
        stats.put("todayOrders", orderRepository.countByCreateTimeAfter(
                LocalDateTime.now().toLocalDate().atStartOfDay()));
        return stats;
    }

    /**
     * 定时取消超时未支付的订单。
     * <p>每 5 分钟执行一次，将超过 30 分钟仍未支付的 pending 订单标记为 cancelled。</p>
     */
    @Scheduled(fixedRate = 300000) // 每5分钟执行一次
    @Transactional
    public void cancelExpiredOrders() {
        LocalDateTime expireTime = LocalDateTime.now().minusMinutes(30);
        List<Order> expiredOrders = orderRepository.findByStatusAndCreateTimeBefore("pending", expireTime);
        for (Order order : expiredOrders) {
            order.setStatus("cancelled");
            order.setUpdateTime(LocalDateTime.now());
        }
        if (!expiredOrders.isEmpty()) {
            orderRepository.saveAll(expiredOrders);
        }
    }
}
