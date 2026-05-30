package org.xxg.backend.backend.mapper;

import org.springframework.data.jpa.repository.JpaRepository;
import org.xxg.backend.backend.entity.CardStatus;
import java.util.Optional;

public interface CardStatusRepository extends JpaRepository<CardStatus, Long> {
    Optional<CardStatus> findByCardHash(String cardHash);
}
