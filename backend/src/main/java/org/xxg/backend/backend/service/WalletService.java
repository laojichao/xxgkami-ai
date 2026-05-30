package org.xxg.backend.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xxg.backend.backend.entity.Wallet;
import org.xxg.backend.backend.entity.WalletTransaction;
import org.xxg.backend.backend.exception.BusinessException;
import org.xxg.backend.backend.mapper.WalletRepository;
import org.xxg.backend.backend.mapper.WalletTransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class WalletService {
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;

    public WalletService(WalletRepository walletRepository, WalletTransactionRepository transactionRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    public Wallet getOrCreateWallet(Integer userId) {
        return walletRepository.findByUserId(userId).orElseGet(() -> {
            Wallet wallet = new Wallet();
            wallet.setUserId(userId);
            wallet.setBalance(BigDecimal.ZERO);
            return walletRepository.save(wallet);
        });
    }

    @Transactional
    public Wallet recharge(Integer userId, BigDecimal amount, String description, String orderNo) {
        Wallet wallet = getOrCreateWallet(userId);
        wallet.setBalance(wallet.getBalance().add(amount));
        wallet.setTotalRecharge(wallet.getTotalRecharge().add(amount));
        wallet.setUpdateTime(LocalDateTime.now());
        walletRepository.save(wallet);

        recordTransaction(userId, "recharge", amount, wallet.getBalance(), description, orderNo);
        return wallet;
    }

    @Transactional
    public Wallet consume(Integer userId, BigDecimal amount, String description, String orderNo) {
        Wallet wallet = getOrCreateWallet(userId);
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new BusinessException("余额不足");
        }
        wallet.setBalance(wallet.getBalance().subtract(amount));
        wallet.setTotalConsume(wallet.getTotalConsume().add(amount));
        wallet.setUpdateTime(LocalDateTime.now());
        walletRepository.save(wallet);

        recordTransaction(userId, "consume", amount, wallet.getBalance(), description, orderNo);
        return wallet;
    }

    private void recordTransaction(Integer userId, String type, BigDecimal amount,
                                   BigDecimal balanceAfter, String description, String orderNo) {
        WalletTransaction tx = new WalletTransaction();
        tx.setUserId(userId);
        tx.setType(type);
        tx.setAmount(amount);
        tx.setBalanceAfter(balanceAfter);
        tx.setDescription(description);
        tx.setOrderNo(orderNo);
        transactionRepository.save(tx);
    }

    public List<WalletTransaction> getTransactions(Integer userId) {
        return transactionRepository.findByUserIdOrderByCreateTimeDesc(userId);
    }

    public Page<WalletTransaction> getTransactions(Integer userId, Pageable pageable) {
        return transactionRepository.findByUserId(userId, pageable);
    }
}
