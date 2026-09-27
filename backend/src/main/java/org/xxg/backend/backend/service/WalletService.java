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
     * 获取用户钱包，不存在则自动创建。
     * <p>注意：本方法不再捕获 {@code DataIntegrityViolationException}。
     * 在事务内捕获约束冲突会把事务标记为 rollback-only，随后的查询/提交会抛出
     * {@code UnexpectedRollbackException}，属于不可恢复状态。并发首次创建钱包是
     * 极小概率事件，冲突会由 {@code GlobalExceptionHandler} 统一转换为 409，
     * 不会返回错误数据。</p>
     *
     * @param userId 用户ID
     * @return 钱包实体
     */
    @Transactional
    public Wallet getOrCreateWallet(Integer userId) {
        return walletRepository.findByUserId(userId).orElseGet(() -> {
            Wallet wallet = new Wallet();
            wallet.setUserId(userId);
            wallet.setBalance(BigDecimal.ZERO);
            wallet.setTotalRecharge(BigDecimal.ZERO);
            wallet.setTotalConsume(BigDecimal.ZERO);
            return walletRepository.save(wallet);
        });
    }

    /**
     * 用户钱包充值。
     * <p><b>安全提示：</b>本方法直接增加余额，不涉及任何支付校验，
     * 仅供受信任的内部流程（如管理员调账、支付网关回调）调用。
     * 对外的自助充值必须经过支付网关下单-回调流程，
     * 严禁将本方法暴露给普通用户直接调用，否则用户可凭空增加余额。</p>
     *
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
        // 直接查询/创建钱包（不使用 getOrCreateWallet，避免同类自调用绕过事务代理）
        Wallet wallet = walletRepository.findByUserId(userId).orElseGet(() -> {
            Wallet created = new Wallet();
            created.setUserId(userId);
            created.setBalance(BigDecimal.ZERO);
            created.setTotalRecharge(BigDecimal.ZERO);
            created.setTotalConsume(BigDecimal.ZERO);
            return walletRepository.save(created);
        });
        return doRecharge(wallet, userId, amount, description, orderNo);
    }

    /**
     * 执行充值核心逻辑（加锁、更新余额、记录流水）。
     * 提取此方法消除 recharge 中的重复代码。
     * <p>余额字段做 null 兜底：历史数据可能因迁移或手工插入而为 NULL，
     * 直接相加会抛 NullPointerException。</p>
     */
    private Wallet doRecharge(Wallet wallet, Integer userId, BigDecimal amount,
                              String description, String orderNo) {
        entityManager.lock(wallet, LockModeType.PESSIMISTIC_WRITE);
        wallet.setBalance(safeAmount(wallet.getBalance()).add(amount));
        wallet.setTotalRecharge(safeAmount(wallet.getTotalRecharge()).add(amount));
        wallet.setUpdateTime(LocalDateTime.now());
        walletRepository.save(wallet);
        recordTransaction(userId, "recharge", amount, wallet.getBalance(), description, orderNo);
        return wallet;
    }

    /** 金额字段 null 兜底，返回零值而非 null */
    private BigDecimal safeAmount(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    /**
     * 用户钱包消费
     * <p>注意：此方法为预留接口，当前业务流程中订单支付通过支付网关完成，
     * 未直接调用此方法扣减钱包余额。未来支持钱包余额购买卡密时可启用。</p>
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

        if (safeAmount(wallet.getBalance()).compareTo(amount) < 0) {
            throw new BusinessException("余额不足");
        }
        wallet.setBalance(safeAmount(wallet.getBalance()).subtract(amount));
        wallet.setTotalConsume(safeAmount(wallet.getTotalConsume()).add(amount));
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
    @Transactional(readOnly = true)
    public List<WalletTransaction> getTransactions(Integer userId) {
        return transactionRepository.findByUserIdOrderByCreateTimeDesc(userId);
    }

    /**
     * 分页查询用户交易记录
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 交易记录分页结果
     */
    @Transactional(readOnly = true)
    public Page<WalletTransaction> getTransactions(Integer userId, Pageable pageable) {
        return transactionRepository.findByUserId(userId, pageable);
    }
}
