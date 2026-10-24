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
        // 仅待支付订单可发起支付：已取消/已完成/已失败的订单继续生成支付链接，
        // 会出现"用户对已取消订单付款 -> 回调无法发货 -> 资金吞单"的资损路径
        if (!"pending".equals(order.getStatus())) {
            throw new BusinessException("订单当前状态（" + order.getStatus() + "）不允许支付");
        }

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
        String payUrl = apiUrl + "submit.php?" + buildQueryString(params);
        // 同时返回 url 与 paymentUrl：前端 (UserPage.vue) 读取 paymentUrl，
        // 旧客户端/文档使用 url。仅返回 url 会导致前端拿不到支付链接、无法跳转支付。
        result.put("url", payUrl);
        result.put("paymentUrl", payUrl);
        return result;
    }

    /**
     * 构建支付跳转 URL（供控制器包装为统一响应格式）。
     *
     * @param orderNo 订单编号
     * @return 支付平台跳转 URL
     */
    public String buildPaymentUrl(String orderNo) {
        return createPayment(orderNo).get("paymentUrl");
    }

    /**
     * 处理易支付异步回调通知
     * 验证签名并完成订单状态更新，包含幂等性保护和卡密生成
     * <p><b>安全提示：</b>支付回调接口应在 nginx/反向代理层配置 IP 白名单，
     * 仅允许支付网关的 IP 访问 /payment/notify，防止伪造回调。
     * 可选：通过环境变量 PAYMENT_CALLBACK_IP_WHITELIST 配置允许的 IP 列表（逗号分隔），
     * 在代码中通过 request.getRemoteAddr() 校验。</p>
     * @param params 回调参数
     * @return "success"表示处理成功，"fail"表示处理失败
     */
    @Transactional
    public String handlePaymentCallback(Map<String, String> params) {
        String key = settingsService.getValue("epay_key", "");
        // 安全修复：支付密钥未配置时一律拒绝回调。
        // 否则 verifySign 会以空字符串作为密钥计算 MD5，攻击者可自行伪造合法签名，
        // 从而把任意订单标记为已支付并白嫖卡密。
        if (key == null || key.isBlank()) {
            log.error("支付回调被拒绝：epay_key 未配置，无法验证签名");
            return "fail";
        }
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
            // 幂等性检查：已完成的订单直接确认（网关重试场景）
            if ("completed".equals(order.getStatus())) {
                log.info("订单已完成，幂等跳过: {}", orderNo);
                return "success";
            }
            // 已取消/失败的订单收到真实付款：绝不能向网关返回 success（网关会停止重试、
            // 系统侧无退款无告警，用户付了钱拿不到卡密）。返回 fail 让网关持续重试，
            // 并输出高级别告警日志驱动人工对账/补发。
            if (!"pending".equals(order.getStatus())) {
                log.error("[支付对账告警] 订单 {} 已被置为 {} 状态但收到金额 {} 的成功回调，"
                        + "请人工核实并补发卡密或退款。回调参数: {}",
                        orderNo, order.getStatus(), params.get("money"), params);
                return "fail";
            }

            // 验证回调金额与订单金额一致性（防止支付金额被篡改）
            String callbackMoney = params.get("money");
            if (callbackMoney == null || callbackMoney.isBlank()) {
                log.warn("支付回调缺少金额参数，订单号: {}", orderNo);
                return "fail";
            }
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
