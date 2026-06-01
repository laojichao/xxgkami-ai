package org.xxg.backend.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.xxg.backend.backend.dto.ApiResponse;
import org.xxg.backend.backend.service.PaymentService;
import java.util.Map;

/**
 * 支付接口
 * <p>提供支付创建、支付回调通知及支付返回处理功能。</p>
 * <p>基础路径：/payment</p>
 */
@RestController
@RequestMapping("/payment")
public class PaymentController {
    private final PaymentService paymentService;
    public PaymentController(PaymentService paymentService) { this.paymentService = paymentService; }

    /**
     * 创建支付订单
     * <p>POST /payment/pay</p>
     * <p>权限：已认证用户</p>
     * @param body 请求体，包含 orderNo（订单号）
     * @return 支付平台返回的支付信息（如支付链接等）
     */
    @PostMapping("/pay")
    public ResponseEntity<Map<String, String>> pay(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(paymentService.createPayment(body.get("orderNo")));
    }

    /**
     * 创建支付订单（别名接口）
     * <p>POST /payment/create</p>
     * <p>权限：已认证用户</p>
     * @param body 请求体，包含 orderNo（订单号）
     * @return 支付平台返回的支付信息
     */
    @PostMapping("/create")
    public ResponseEntity<Map<String, String>> createPayment(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(paymentService.createPayment(body.get("orderNo")));
    }

    /**
     * 支付回调通知（由支付平台主动调用）
     * <p>POST /payment/notify</p>
     * <p>权限：公开访问（支付平台回调，通过签名验证安全性）</p>
     * <p>接收支付平台的异步通知，验证签名后更新订单状态。</p>
     * @param params 支付平台传入的回调参数
     * @return 处理结果字符串
     */
    @PostMapping("/notify")
    public String notify(@RequestParam Map<String, String> params) {
        return paymentService.handlePaymentCallback(params);
    }

    /**
     * 支付完成后前端跳转回调
     * <p>GET /payment/return</p>
     * <p>权限：公开访问</p>
     * <p>用户在支付平台完成支付后跳转回本站的同步回调地址。</p>
     * @param params 回调参数
     * @return 固定返回 "success"
     */
    @GetMapping("/return")
    public String returnUrl(@RequestParam Map<String, String> params) {
        return "success";
    }
}
