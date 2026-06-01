package org.xxg.backend.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xxg.backend.backend.entity.Order;
import org.xxg.backend.backend.entity.Setting;
import org.xxg.backend.backend.exception.BusinessException;
import org.xxg.backend.backend.mapper.OrderRepository;
import org.xxg.backend.backend.mapper.SettingRepository;
import org.xxg.backend.backend.util.PaymentUtil;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 支付服务（易支付对接）
 * 处理支付订单的创建、支付回调验证等支付相关业务逻辑
 */
@Service
public class PaymentService {

    private final SettingRepository settingRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final CardService cardService;
    private final PaymentUtil paymentUtil;

    public PaymentService(SettingRepository settingRepository, OrderRepository orderRepository,
                          OrderService orderService, CardService cardService, PaymentUtil paymentUtil) {
        this.settingRepository = settingRepository;
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

        String apiUrl = getSettingValue("epay_api_url", "");
        String pid = getSettingValue("epay_pid", "");
        String key = getSettingValue("epay_key", "");
        String notifyUrl = getSettingValue("epay_notify_url", "");
        String returnUrl = getSettingValue("epay_return_url", "");

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
     * 验证签名并完成订单状态更新
     * @param params 回调参数
     * @return "success"表示处理成功，"fail"表示处理失败
     */
    @Transactional
    public String handlePaymentCallback(Map<String, String> params) {
        String key = getSettingValue("epay_key", "");
        if (!paymentUtil.verifySign(params, key)) {
            return "fail";
        }

        String tradeStatus = params.get("trade_status");
        String orderNo = params.get("out_trade_no");

        if ("TRADE_SUCCESS".equals(tradeStatus)) {
            Order order = orderRepository.findByOrderNo(orderNo).orElse(null);
            if (order != null && "pending".equals(order.getStatus())) {
                orderService.completeOrder(orderNo, null);
            }
            return "success";
        }
        return "fail";
    }

    /**
     * 从数据库获取系统配置项的值
     * @param name 配置项名称
     * @param defaultValue 默认值
     * @return 配置项值，不存在返回默认值
     */
    private String getSettingValue(String name, String defaultValue) {
        return settingRepository.findByName(name)
                .map(Setting::getValue)
                .orElse(defaultValue);
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
