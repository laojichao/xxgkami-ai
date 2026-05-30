package org.xxg.backend.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.xxg.backend.backend.dto.ApiResponse;
import org.xxg.backend.backend.service.PaymentService;
import java.util.Map;

@RestController
@RequestMapping("/payment")
public class PaymentController {
    private final PaymentService paymentService;
    public PaymentController(PaymentService paymentService) { this.paymentService = paymentService; }

    @PostMapping("/pay")
    public ResponseEntity<Map<String, String>> pay(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(paymentService.createPayment(body.get("orderNo")));
    }

    @PostMapping("/create")
    public ResponseEntity<Map<String, String>> createPayment(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(paymentService.createPayment(body.get("orderNo")));
    }

    @PostMapping("/notify")
    public String notify(@RequestParam Map<String, String> params) {
        return paymentService.handlePaymentCallback(params);
    }

    @GetMapping("/return")
    public String returnUrl(@RequestParam Map<String, String> params) {
        return "success";
    }
}
