package org.xxg.backend.backend.mapper;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.xxg.backend.backend.entity.Card;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 卡密数据访问接口
 * 提供卡密的增删改查、按卡密键/加密键/状态/创建者/机器码查询以及多维度统计功能
 */
public interface CardRepository extends JpaRepository<Card, Integer> {
    /** 根据卡密明文键查询卡密 */
    Optional<Card> findByCardKey(String cardKey);
    /** 根据卡密明文键查询卡密（悲观锁，用于并发安全场景） */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Card c WHERE c.cardKey = :cardKey")
    Optional<Card> findByCardKeyForUpdate(@Param("cardKey") String cardKey);
    /** 根据加密键查询卡密 */
    Optional<Card> findByEncryptedKey(String encryptedKey);
    /** 根据创建者类型和创建者ID查询卡密列表 */
    List<Card> findByCreatorTypeAndCreatorId(Card.CreatorType creatorType, Integer creatorId);
    /** 根据创建者类型查询卡密列表（不限创建者ID） */
    List<Card> findByCreatorType(Card.CreatorType creatorType);
    /** 根据创建者类型和创建者ID分页查询卡密 */
    Page<Card> findByCreatorTypeAndCreatorId(Card.CreatorType creatorType, Integer creatorId, Pageable pageable);
    /** 根据状态查询卡密列表 */
    List<Card> findByStatus(Integer status);
    /** 统计指定状态的卡密数量 */
    long countByStatus(Integer status);
    /** 统计指定时间之后创建的卡密数量 */
    long countByCreateTimeAfter(LocalDateTime time);
    /** 统计指定卡密类型的数量 */
    long countByCardType(Card.CardType cardType);
    /** 统计指定创建者类型的卡密数量 */
    long countByCreatorType(Card.CreatorType creatorType);
    /** 分页查询所有卡密 */
    Page<Card> findAll(Pageable pageable);
    /** 根据机器码查询卡密列表 */
    List<Card> findByMachineCode(String machineCode);
    /** 根据卡密ID和创建者ID查询卡密（权限校验用） */
    Optional<Card> findByIdAndCreatorId(Integer id, Integer creatorId);
    /** 根据API Key ID查询卡密列表 */
    List<Card> findByApiKeyId(Integer apiKeyId);
    /** 统计指定时间范围内按天分组的卡密使用数量 */
    @Query("SELECT FUNCTION('DATE', c.useTime) as day, COUNT(c) FROM Card c WHERE c.useTime >= :startTime AND c.status = 1 GROUP BY FUNCTION('DATE', c.useTime) ORDER BY day")
    List<Object[]> countUsedCardsGroupByDay(@Param("startTime") LocalDateTime startTime);
}
