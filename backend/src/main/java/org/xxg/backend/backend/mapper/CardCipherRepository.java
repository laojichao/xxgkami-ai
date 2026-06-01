package org.xxg.backend.backend.mapper;

import org.springframework.data.jpa.repository.JpaRepository;
import org.xxg.backend.backend.entity.CardCipher;
import java.util.Optional;

/**
 * 卡密加密数据访问接口
 * 提供卡密加密信息的增删改查及按哈希值查询和唯一性校验功能
 */
public interface CardCipherRepository extends JpaRepository<CardCipher, Long> {
    /** 根据卡密哈希值查询加密记录 */
    Optional<CardCipher> findByCardHash(String cardHash);
    /** 判断指定卡密哈希值是否存在 */
    boolean existsByCardHash(String cardHash);
}
