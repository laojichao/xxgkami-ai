package org.xxg.backend.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xxg.backend.backend.dto.CreateOrderRequest;
import org.xxg.backend.backend.entity.*;
import org.xxg.backend.backend.exception.BusinessException;
import org.xxg.backend.backend.mapper.*;
import org.xxg.backend.backend.util.PaymentUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

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

    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        // Validate user exists
        if (request.getUserId() != null) {
            userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new BusinessException("用户不存在"));
        }

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

    @Transactional
    public void completeOrder(String orderNo, String cardKeys) {
        Order order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new BusinessException("订单不存在"));
        order.setStatus("completed");
        order.setPayTime(LocalDateTime.now());
        order.setCardKeys(cardKeys);
        orderRepository.save(order);
    }

    @Transactional
    public void failOrder(String orderNo) {
        Order order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new BusinessException("订单不存在"));
        order.setStatus("failed");
        orderRepository.save(order);
    }

    public Order getOrderByNo(String orderNo) {
        return orderRepository.findByOrderNo(orderNo).orElse(null);
    }

    public List<Order> getOrdersByUserId(Integer userId) {
        return orderRepository.findByUserId(userId);
    }

    public Page<Order> getOrdersByUserId(Integer userId, Pageable pageable) {
        return orderRepository.findByUserId(userId, pageable);
    }

    public Page<Order> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable);
    }

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
}
