package org.xxg.backend.backend.mapper;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.xxg.backend.backend.entity.Wallet;
import java.util.Optional;

/**
 * 钱包数据访问接口
 * 提供用户钱包的增删改查及按用户ID查询功能
 */
public interface WalletRepository extends JpaRepository<Wallet, Long> {
    /** 根据用户ID查询钱包 */
    Optional<Wallet> findByUserId(Integer userId);

    /**
     * 根据用户ID锁定读钱包（SELECT ... FOR UPDATE）。
     * <p>余额变更必须通过锁定读获取最新状态：先普通读再 entityManager.lock
     * 不会刷新持久化上下文，事务仍按加锁前的快照计算余额，并发充值会互相覆盖（丢失更新）。</p>
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.userId = :userId")
    Optional<Wallet> findByUserIdForUpdate(@Param("userId") Integer userId);

    /** 根据用户ID删除钱包 */
    void deleteByUserId(Integer userId);
}
