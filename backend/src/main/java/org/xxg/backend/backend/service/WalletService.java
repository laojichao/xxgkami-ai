package org.xxg.backend.backend.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xxg.backend.backend.entity.Wallet;
import org.xxg.backend.backend.entity.WalletTransaction;
import org.xxg.backend.backend.exception.BusinessException;
import org.xxg.backend.backend.mapper.WalletRepository;
import org.xxg.backend.backend.mapper.WalletTransactionRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 钱包服务
 * 管理用户钱包余额，提供充值、消费、交易记录查询等功能
 */
@Service
public class WalletService {
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    @PersistenceContext
    private EntityManager entityManager;

    public WalletService(WalletRepository walletRepository, WalletTransactionRepository transactionRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    /**
     * 获取用户钱包，不存在则自动创建
     * @param userId 用户ID
     * @return 钱包实体
     */
    public Wallet getOrCreateWallet(Integer userId) {
        return walletRepository.findByUserId(userId).orElseGet(() -> {
            Wallet wallet = new Wallet();
            wallet.setUserId(userId);
            wallet.setBalance(BigDecimal.ZERO);
            return walletRepository.save(wallet);
        });
    }

    /**
     * 用户钱包充值
     * @param userId 用户ID
     * @param amount 充值金额
     * @param description 交易描述
     * @param orderNo 关联订单号
     * @return 充值后的钱包实体
     */
    @Transactional
    public Wallet recharge(Integer userId, BigDecimal amount, String description, String orderNo) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("充值金额必须大于0");
        }
        try {
            // 先查询钱包，不存在则创建，避免 orElseGet 中 save 与后续 lock 的竞态条件
            Wallet wallet = walletRepository.findByUserId(userId).orElse(null);
            if (wallet == null) {
                wallet = new Wallet();
                wallet.setUserId(userId);
                wallet.setBalance(BigDecimal.ZERO);
                wallet = walletRepository.save(wallet);
            }
            entityManager.lock(wallet, LockModeType.PESSIMISTIC_WRITE);
            wallet.setBalance(wallet.getBalance().add(amount));
            wallet.setTotalRecharge(wallet.getTotalRecharge().add(amount));
            wallet.setUpdateTime(LocalDateTime.now());
            walletRepository.save(wallet);

            recordTransaction(userId, "recharge", amount, wallet.getBalance(), description, orderNo);
            return wallet;
        } catch (DataIntegrityViolationException e) {
            // 处理并发创建钱包的极端情况：另一个线程已创建了钱包，重新查询即可
            Wallet wallet = walletRepository.findByUserId(userId)
                    .orElseThrow(() -> new BusinessException("钱包创建失败"));
            entityManager.lock(wallet, LockModeType.PESSIMISTIC_WRITE);
            wallet.setBalance(wallet.getBalance().add(amount));
            wallet.setTotalRecharge(wallet.getTotalRecharge().add(amount));
            wallet.setUpdateTime(LocalDateTime.now());
            walletRepository.save(wallet);

            recordTransaction(userId, "recharge", amount, wallet.getBalance(), description, orderNo);
            return wallet;
        }
    }

    /**
     * 用户钱包消费
     * @param userId 用户ID
     * @param amount 消费金额
     * @param description 交易描述
     * @param orderNo 关联订单号
     * @return 消费后的钱包实体
     * @throws BusinessException 余额不足时抛出异常
     */
    @Transactional
    public Wallet consume(Integer userId, BigDecimal amount, String description, String orderNo) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("消费金额必须大于0");
        }
        // 使用悲观锁防止并发消费导致余额为负
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("钱包不存在"));
        entityManager.lock(wallet, LockModeType.PESSIMISTIC_WRITE);

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

    /**
     * 记录钱包交易流水
     * @param userId 用户ID
     * @param type 交易类型：recharge-充值，consume-消费
     * @param amount 交易金额
     * @param balanceAfter 交易后余额
     * @param description 交易描述
     * @param orderNo 关联订单号
     */
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

    /**
     * 获取用户交易记录列表（按时间倒序）
     * @param userId 用户ID
     * @return 交易记录列表
     */
    public List<WalletTransaction> getTransactions(Integer userId) {
        return transactionRepository.findByUserIdOrderByCreateTimeDesc(userId);
    }

    /**
     * 分页查询用户交易记录
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 交易记录分页结果
     */
    public Page<WalletTransaction> getTransactions(Integer userId, Pageable pageable) {
        return transactionRepository.findByUserId(userId, pageable);
    }
}
