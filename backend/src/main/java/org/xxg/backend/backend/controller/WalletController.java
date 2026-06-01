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

/**
 * 钱包接口
 * <p>提供用户钱包余额查询、余额充值及交易记录查询功能。</p>
 * <p>基础路径：/wallet</p>
 * <p>权限：已认证用户（仅可操作自己的钱包）</p>
 */
@RestController
@RequestMapping("/wallet")
public class WalletController {
    private final WalletService walletService;
    private final UserRepository userRepository;

    public WalletController(WalletService walletService, UserRepository userRepository) {
        this.walletService = walletService;
        this.userRepository = userRepository;
    }

    /**
     * 获取当前用户的钱包信息
     * <p>GET /wallet</p>
     * <p>权限：已认证用户</p>
     * <p>若用户尚未有钱包记录，会自动创建。</p>
     * @param auth Spring Security认证对象，用于获取当前用户ID
     * @return 钱包信息（包含余额等）
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Wallet>> getWallet(Authentication auth) {
        Integer userId = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new org.xxg.backend.backend.exception.BusinessException("用户不存在")).getId();
        return ResponseEntity.ok(ApiResponse.ok(walletService.getOrCreateWallet(userId)));
    }

    /**
     * 用户余额充值
     * <p>POST /wallet/recharge</p>
     * <p>权限：已认证用户</p>
     * <p>充值金额限制：必须大于0，单次不超过100,000。</p>
     * @param auth Spring Security认证对象，用于获取当前用户ID
     * @param body 请求体，包含 amount（充值金额）字段
     * @return 充值后的钱包信息
     */
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

    /**
     * 获取当前用户的交易记录
     * <p>GET /wallet/transactions</p>
     * <p>权限：已认证用户</p>
     * @param auth Spring Security认证对象，用于获取当前用户ID
     * @return 该用户的钱包交易记录列表
     */
    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<List<WalletTransaction>>> getTransactions(Authentication auth) {
        Integer userId = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new org.xxg.backend.backend.exception.BusinessException("用户不存在")).getId();
        return ResponseEntity.ok(ApiResponse.ok(walletService.getTransactions(userId)));
    }
}
