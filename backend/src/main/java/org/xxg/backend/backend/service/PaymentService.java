package org.xxg.backend.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xxg.backend.backend.entity.Order;
import org.xxg.backend.backend.exception.BusinessException;
import org.xxg.backend.backend.mapper.OrderRepository;
import org.xxg.backend.backend.util.PaymentUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 支付服务（易支付对接）
 * 处理支付订单的创建、支付回调验证等支付相关业务逻辑
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final SettingsService settingsService;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final CardService cardService;
    private final PaymentUtil paymentUtil;

    public PaymentService(SettingsService settingsService, OrderRepository orderRepository,
                          OrderService orderService, CardService cardService, PaymentUtil paymentUtil) {
        this.settingsService = settingsService;
        this.orderRepository = orderRepository;
        this.orderService = orderService;
        this.cardService = cardService;
        this.paymentUtil = paymentUtil;
    }

    /**
     * 创建支付订单，生成易支付跳转URL
     * @param orderNo 订单编号
     * @return 包含支付跳转URL的Map
     */
    public Map<String, String> createPayment(String orderNo) {
        Order order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new BusinessException("订单不存在"));

        String apiUrl = settingsService.getValue("epay_api_url", "");
        String pid = settingsService.getValue("epay_pid", "");
        String key = settingsService.getValue("epay_key", "");
        String notifyUrl = settingsService.getValue("epay_notify_url", "");
        String returnUrl = settingsService.getValue("epay_return_url", "");

        if (apiUrl.isEmpty() || pid.isEmpty() || key.isEmpty()) {
            throw new BusinessException("支付配置不完整");
        }

        Map<String, String> params = new TreeMap<>();
        params.put("pid", pid);
        params.put("type", order.getPaymentMethod());
        params.put("out_trade_no", orderNo);
        params.put("notify_url", notifyUrl);
        params.put("return_url", returnUrl);
        params.put("name", "卡密购买 - " + order.getCardType());
        params.put("money", order.getTotalPrice().toPlainString());

        String sign = paymentUtil.generateSign(params, key);
        params.put("sign", sign);
        params.put("sign_type", "MD5");

        Map<String, String> result = new HashMap<>();
        result.put("url", apiUrl + "submit.php?" + buildQueryString(params));
        return result;
    }

    /**
     * 处理易支付异步回调通知
     * 验证签名并完成订单状态更新，包含幂等性保护和卡密生成
     * @param params 回调参数
     * @return "success"表示处理成功，"fail"表示处理失败
     */
    @Transactional
    public String handlePaymentCallback(Map<String, String> params) {
        String key = settingsService.getValue("epay_key", "");
        if (!paymentUtil.verifySign(params, key)) {
            log.warn("支付回调签名校验失败");
            return "fail";
        }

        String tradeStatus = params.get("trade_status");
        String orderNo = params.get("out_trade_no");

        // 防止 orderNo 为空导致异常
        if (orderNo == null || orderNo.isBlank()) {
            log.warn("支付回调缺少订单号参数");
            return "fail";
        }

        if ("TRADE_SUCCESS".equals(tradeStatus)) {
            // 使用悲观锁防止并发重复处理（幂等性保护）
            Order order = orderRepository.findByOrderNoWithLock(orderNo).orElse(null);
            if (order == null) {
                log.warn("支付回调订单不存在: {}", orderNo);
                return "fail";
            }
            // 幂等性检查：只有待支付状态的订单才处理
            if (!"pending".equals(order.getStatus())) {
                log.info("订单已处理过，跳过: {}，当前状态: {}", orderNo, order.getStatus());
                return "success";
            }

            // 验证回调金额与订单金额一致性（防止支付金额被篡改）
            String callbackMoney = params.get("money");
            if (callbackMoney != null && !callbackMoney.isBlank()) {
                try {
                    BigDecimal paidAmount = new BigDecimal(callbackMoney);
                    if (paidAmount.compareTo(order.getTotalPrice()) != 0) {
                        log.warn("支付回调金额不匹配，订单号: {}, 回调金额: {}, 订单金额: {}",
                                orderNo, paidAmount, order.getTotalPrice());
                        return "fail";
                    }
                } catch (NumberFormatException e) {
                    log.warn("支付回调金额格式异常，订单号: {}, money: {}", orderNo, callbackMoney);
                    return "fail";
                }
            }

            // 根据订单信息生成卡密
            String cardKeys = cardService.generateCardsForOrder(order);
            orderService.completeOrder(orderNo, cardKeys);
            log.info("订单支付完成: {}，生成卡密数量: {}", orderNo, order.getQuantity());
            return "success";
        }
        return "fail";
    }

    /**
     * 将参数Map拼接为URL查询字符串
     * @param params 参数Map
     * @return URL编码后的查询字符串
     */
    private String buildQueryString(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (sb.length() > 0) sb.append("&");
            sb.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
              .append("=")
              .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        return sb.toString();
    }
}
