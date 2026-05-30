package org.xxg.backend.backend.mapper;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.xxg.backend.backend.entity.WalletTransaction;
import java.util.List;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
    List<WalletTransaction> findByUserIdOrderByCreateTimeDesc(Integer userId);
    Page<WalletTransaction> findByUserId(Integer userId, Pageable pageable);
    List<WalletTransaction> findByUserIdAndType(Integer userId, String type);
}
