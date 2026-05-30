package org.xxg.backend.backend.mapper;

import org.springframework.data.jpa.repository.JpaRepository;
import org.xxg.backend.backend.entity.CardCipher;
import java.util.Optional;

public interface CardCipherRepository extends JpaRepository<CardCipher, Long> {
    Optional<CardCipher> findByCardHash(String cardHash);
    boolean existsByCardHash(String cardHash);
}
