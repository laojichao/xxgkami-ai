package org.xxg.backend.backend.mapper;

import org.springframework.data.jpa.repository.JpaRepository;
import org.xxg.backend.backend.entity.CardStatus;
import java.util.Optional;

/**
 * 卡密状态数据访问接口
 * 提供卡密状态信息的增删改查及按哈希值查询功能
 */
public interface CardStatusRepository extends JpaRepository<CardStatus, Long> {
    /** 根据卡密哈希值查询卡密状态 */
    Optional<CardStatus> findByCardHash(String cardHash);
}
