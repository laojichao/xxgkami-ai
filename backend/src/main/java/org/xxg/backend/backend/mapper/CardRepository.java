package org.xxg.backend.backend.mapper;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.xxg.backend.backend.entity.Card;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, Integer> {
    Optional<Card> findByCardKey(String cardKey);
    Optional<Card> findByEncryptedKey(String encryptedKey);
    List<Card> findByCreatorTypeAndCreatorId(Card.CreatorType creatorType, Integer creatorId);
    Page<Card> findByCreatorTypeAndCreatorId(Card.CreatorType creatorType, Integer creatorId, Pageable pageable);
    List<Card> findByStatus(Integer status);
    long countByStatus(Integer status);
    long countByCreateTimeAfter(LocalDateTime time);
    long countByCardType(Card.CardType cardType);
    long countByCreatorType(Card.CreatorType creatorType);
    Page<Card> findAll(Pageable pageable);
    List<Card> findByMachineCode(String machineCode);
    Optional<Card> findByIdAndCreatorId(Integer id, Integer creatorId);
}
