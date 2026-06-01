package org.xxg.backend.backend.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.xxg.backend.backend.dto.ApiResponse;

/**
 * Webhook测试接口
 * <p>用于开发和调试阶段测试Webhook回调的连通性。</p>
 * <p>基础路径：/webhook-test</p>
 * <p>权限：公开访问（仅用于测试，接收任意JSON请求体并记录日志）</p>
 */
@RestController
@RequestMapping("/webhook-test")
public class WebhookTestController {

    private static final Logger log = LoggerFactory.getLogger(WebhookTestController.class);

    /**
     * 测试Webhook回调
     * <p>POST /webhook-test</p>
     * <p>权限：公开访问</p>
     * <p>接收Webhook请求并将请求体内容记录到日志，可用于验证第三方平台的Webhook推送是否正常。</p>
     * @param body 任意JSON格式的请求体
     * @return 固定返回成功响应
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> testWebhook(@RequestBody java.util.Map<String, Object> body) {
        log.info("[WEBHOOK TEST] Received: {}", body);
        return ResponseEntity.ok(ApiResponse.ok("Webhook 测试成功"));
    }
}
