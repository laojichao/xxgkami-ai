package org.xxg.backend.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.xxg.backend.backend.dto.ApiResponse;
import org.xxg.backend.backend.entity.Wallet;
import org.xxg.backend.backend.entity.WalletTransaction;
import org.xxg.backend.backend.mapper.UserRepository;
import org.xxg.backend.backend.service.WalletService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/wallet")
public class WalletController {
    private final WalletService walletService;
    private final UserRepository userRepository;

    public WalletController(WalletService walletService, UserRepository userRepository) {
        this.walletService = walletService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Wallet>> getWallet(Authentication auth) {
        Integer userId = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new org.xxg.backend.backend.exception.BusinessException("用户不存在")).getId();
        return ResponseEntity.ok(ApiResponse.ok(walletService.getOrCreateWallet(userId)));
    }

    @PostMapping("/recharge")
    public ResponseEntity<ApiResponse<Wallet>> recharge(Authentication auth, @RequestBody Map<String, Object> body) {
        Integer userId = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new org.xxg.backend.backend.exception.BusinessException("用户不存在")).getId();
        Object amountObj = body.get("amount");
        if (amountObj == null) {
            throw new org.xxg.backend.backend.exception.BusinessException("金额不能为空");
        }
        BigDecimal amount;
        try {
            amount = new BigDecimal(amountObj.toString());
        } catch (NumberFormatException e) {
            throw new org.xxg.backend.backend.exception.BusinessException("金额格式无效");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new org.xxg.backend.backend.exception.BusinessException("充值金额必须大于0");
        }
        if (amount.compareTo(new BigDecimal("100000")) > 0) {
            throw new org.xxg.backend.backend.exception.BusinessException("单次充值金额不能超过100,000");
        }
        return ResponseEntity.ok(ApiResponse.ok(walletService.recharge(userId, amount, "余额充值", null)));
    }

    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<List<WalletTransaction>>> getTransactions(Authentication auth) {
        Integer userId = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new org.xxg.backend.backend.exception.BusinessException("用户不存在")).getId();
        return ResponseEntity.ok(ApiResponse.ok(walletService.getTransactions(userId)));
    }
}
