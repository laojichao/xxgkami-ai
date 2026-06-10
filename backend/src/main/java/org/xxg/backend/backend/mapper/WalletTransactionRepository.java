package org.xxg.backend.backend.mapper;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.xxg.backend.backend.entity.WalletTransaction;
import java.util.List;

/**
 * 钱包交易记录数据访问接口
 * 提供钱包交易记录的增删改查及按用户ID、交易类型查询功能
 */
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
    /** 根据用户ID查询交易记录，按创建时间降序排列 */
    List<WalletTransaction> findByUserIdOrderByCreateTimeDesc(Integer userId);
    /** 根据用户ID分页查询交易记录 */
    Page<WalletTransaction> findByUserId(Integer userId, Pageable pageable);
    /** 根据用户ID和交易类型查询交易记录 */
    List<WalletTransaction> findByUserIdAndType(Integer userId, String type);
    /** 根据用户ID删除交易记录 */
    void deleteByUserId(Integer userId);
}
