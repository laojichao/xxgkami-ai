package org.xxg.backend.backend.mapper;

import org.springframework.data.jpa.repository.JpaRepository;
import org.xxg.backend.backend.entity.Wallet;
import java.util.Optional;

/**
 * 钱包数据访问接口
 * 提供用户钱包的增删改查及按用户ID查询功能
 */
public interface WalletRepository extends JpaRepository<Wallet, Long> {
    /** 根据用户ID查询钱包 */
    Optional<Wallet> findByUserId(Integer userId);

    /** 根据用户ID删除钱包 */
    void deleteByUserId(Integer userId);
}
