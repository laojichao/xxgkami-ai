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

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OrderService.class);

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

        // 安全修复：校验 pricingId 对应的卡密类型与请求的 cardType 一致，防止价格混淆攻击
        if (pricing.getType() == null || !pricing.getType().equals(request.getCardType())) {
            throw new BusinessException("价格配置与卡密类型不匹配");
        }

        Order order = new Order();
        order.setOrderNo(paymentUtil.generateOrderNo());
        order.setUserId(request.getUserId());
        order.setUsername(request.getUsername());
        order.setCardType(request.getCardType());
        // 安全修复：卡密规格必须由服务端根据定价配置推导，绝不能采信客户端传入的值。
        // 历史缺陷：直接存储 request.getCardSpec()，而该字段随后会被
        // CardService.generateCardsForOrder 解析为卡密天数/次数，
        // 攻击者可提交 cardSpec="9999天" 搭配最便宜的定价，花 1 天卡的钱买到 9999 天卡。
        order.setCardSpec(buildCardSpec(pricing));
        order.setQuantity(request.getQuantity());
        order.setUnitPrice(pricing.getPrice());
        order.setTotalPrice(pricing.getPrice().multiply(BigDecimal.valueOf(request.getQuantity())));
        order.setStatus("pending");
        order.setPaymentMethod(request.getPaymentMethod());
        order.setCreateTime(LocalDateTime.now());

        return orderRepository.save(order);
    }

    /**
     * 根据定价配置生成卡密规格字符串（服务端权威数据）。
     * <p>格式与 {@code CardService.parseDaysFromSpec/parseCountFromSpec} 的解析规则对齐：
     * 时长卡为「N天」，次数卡为「N次」。前端展示的 {@code CardPricing.description}
     * 仅用于 UI，不参与任何价格或规格计算。</p>
     *
     * @param pricing 已校验的定价配置
     * @return 规格字符串，例如 "30天" 或 "100次"
     */
    private String buildCardSpec(CardPricing pricing) {
        Integer value = pricing.getValue();
        if (value == null || value <= 0) {
            throw new BusinessException("价格配置的规格值无效");
        }
        return "time".equals(pricing.getType()) ? value + "天" : value + "次";
    }

    /**
     * 完成订单（支付成功后调用）。
     *
     * @param orderNo  订单号
     * @param cardKeys 生成的卡密明文（多个以逗号分隔），为 null 时自动生成
     */
    @Transactional
    public void completeOrder(String orderNo, String cardKeys) {
        // 使用悲观锁读取订单：本方法同时被支付回调（/payment/notify）和管理员手动完成
        // （/orders/admin/updateStatus）调用。若不加锁，两条路径并发执行时可能同时通过
        // 下方的 pending 状态检查，各自调用 generateCardsForOrder 生成一遍卡密，
        // 造成卡密重复发放（用户白得一份卡密）。
        Order order = orderRepository.findByOrderNoWithLock(orderNo)
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
        // 安全修复：cardKeys 为 null 时自动生成卡密，防止管理员手动完成订单导致卡密缺失
        String finalCardKeys = cardKeys;
        if (finalCardKeys == null || finalCardKeys.isBlank()) {
            finalCardKeys = cardService.generateCardsForOrder(order);
        }
        order.setCardKeys(finalCardKeys);
        orderRepository.save(order);
        // 投递卡密到用户邮箱：前端在购买成功提示中明确告知「已发送订单通知邮件」，
        // 但此前 EmailService 虽已注入却从未调用，用户实际收不到任何邮件。
        // 邮件为异步发送且失败不抛异常，不影响订单完成这一核心事务。
        sendCardKeysByEmail(order, finalCardKeys);
    }

    /**
     * 将订单卡密发送到用户账号邮箱（异步，失败仅记录日志）。
     * <p>收件地址从 users 表按订单 userId 实时查询，而非采信下单时客户端传入的
     * {@code CreateOrderRequest.email}，避免卡密被投递到攻击者指定的邮箱。
     * 账号未绑定邮箱时跳过，不视为错误。</p>
     *
     * @param order    已完成订单
     * @param cardKeys 卡密明文（多个以逗号分隔）
     */
    private void sendCardKeysByEmail(Order order, String cardKeys) {
        if (cardKeys == null || cardKeys.isBlank() || order.getUserId() == null) {
            return;
        }
        try {
            String email = userRepository.findById(order.getUserId())
                    .map(User::getEmail)
                    .orElse(null);
            if (email == null || email.isBlank()) {
                return;
            }
            emailService.sendCardKeys(email, cardKeys);
        } catch (Exception e) {
            // 邮件投递失败不影响订单状态；用户仍可在「我的订单」中查看卡密
            log.warn("订单 {} 卡密邮件发送失败: {}", order.getOrderNo(), e.getMessage());
        }
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
                predicates.add(cb.like(root.get("orderNo"), "%" + escapeLike(orderNo) + "%", '\\'));
            }
            if (username != null && !username.isBlank()) {
                predicates.add(cb.like(root.get("username"), "%" + escapeLike(username) + "%", '\\'));
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
        int cancelled = orderRepository.batchCancelExpiredOrders(
                "cancelled", LocalDateTime.now(), "pending", expireTime);
        if (cancelled > 0) {
            log.info("已自动取消 {} 个超时订单", cancelled);
        }
    }

    /**
     * 转义 LIKE 查询中的特殊字符（%、_、\）。
     */
    private String escapeLike(String value) {
        if (value == null) return null;
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
