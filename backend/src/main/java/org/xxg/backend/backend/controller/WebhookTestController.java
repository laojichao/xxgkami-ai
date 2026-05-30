package org.xxg.backend.backend.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.xxg.backend.backend.dto.ApiResponse;

@RestController
@RequestMapping("/webhook-test")
public class WebhookTestController {

    private static final Logger log = LoggerFactory.getLogger(WebhookTestController.class);

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> testWebhook(@RequestBody java.util.Map<String, Object> body) {
        log.info("[WEBHOOK TEST] Received: {}", body);
        return ResponseEntity.ok(ApiResponse.ok("Webhook 测试成功"));
    }
}
