package org.xxg.backend.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xxg.backend.backend.entity.*;
import org.xxg.backend.backend.exception.BusinessException;
import org.xxg.backend.backend.mapper.*;
import org.xxg.backend.backend.util.AdvancedCryptoUtil;
import org.xxg.backend.backend.util.CustomCardObfuscator;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 卡密业务服务。
 * <p>提供卡密的生成、验证、绑定/解绑机器码、启停用、删除及统计等功能。
 * 卡密分为时长卡和次数卡两种类型，支持 AES-GCM 加密存储和 HMAC 签名校验。</p>
 */
@Service
public class CardService {

    private final CardRepository cardRepository;
    private final CardCipherRepository cardCipherRepository;
    private final CardStatusRepository cardStatusRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final AdvancedCryptoUtil cryptoUtil;
    private final CustomCardObfuscator obfuscator;
    private final WebhookService webhookService;

    public CardService(CardRepository cardRepository, CardCipherRepository cardCipherRepository,
                       CardStatusRepository cardStatusRepository, ApiKeyRepository apiKeyRepository,
                       AdvancedCryptoUtil cryptoUtil, CustomCardObfuscator obfuscator,
                       WebhookService webhookService) {
        this.cardRepository = cardRepository;
        this.cardCipherRepository = cardCipherRepository;
        this.cardStatusRepository = cardStatusRepository;
        this.apiKeyRepository = apiKeyRepository;
        this.cryptoUtil = cryptoUtil;
        this.obfuscator = obfuscator;
        this.webhookService = webhookService;
    }

    /**
     * 生成卡密。
     * <p>创建卡密主记录、加密数据（Cipher）和状态信息，支持时长卡和次数卡。</p>
     *
     * @param cardType     卡密类型（time/count）
     * @param duration     时长（分钟）
     * @param totalCount   总次数（次数卡）
     * @param creatorType  创建者类型（admin/user/system）
     * @param creatorId    创建者 ID
     * @param creatorName  创建者名称
     * @param verifyMethod 验证方式
     * @param days         有效天数（时长卡）
     * @param apiKeyId     关联的 API Key ID
     * @return 生成的卡密实体
     * @throws Exception 加密过程中的异常
     */
    @Transactional
    public Card generateCard(String cardType, Integer duration, Integer totalCount,
                             String creatorType, Integer creatorId, String creatorName,
                             String verifyMethod, Integer days, Long apiKeyId) throws Exception {
        // 校验枚举参数
        Card.CardType type;
        Card.CreatorType creator;
        try {
            type = Card.CardType.valueOf(cardType);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("无效的卡密类型: " + cardType);
        }
        try {
            creator = Card.CreatorType.valueOf(creatorType);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("无效的创建者类型: " + creatorType);
        }

        String cardKey = obfuscator.generateCardKey();
        String encryptedKey = obfuscator.generateEncryptedKey(cardKey);

        // Create cipher data
        String aesKey = cryptoUtil.generateAesKey();
        String iv = cryptoUtil.generateIv();
        String cipherData = cryptoUtil.encrypt(cardKey, aesKey, iv);
        String signData = cryptoUtil.hmacSign(cardKey, aesKey);
        String cardHash = encryptedKey;

        CardCipher cipher = new CardCipher();
        cipher.setCardHash(cardHash);
        cipher.setCipherData(cipherData);
        cipher.setSignData(signData);
        cipher.setSalt("global");
        cipher.setIv(iv);
        cardCipherRepository.save(cipher);

        // Create card status
        CardStatus status = new CardStatus();
        status.setCardHash(cardHash);
        status.setIsValid(true);
        if ("time".equals(cardType) && days != null) {
            status.setExpireTime(LocalDateTime.now().plusDays(days));
        }
        if ("count".equals(cardType) && totalCount != null) {
            status.setTotalCount(totalCount);
            status.setRemainCount(totalCount);
        }
        cardStatusRepository.save(status);

        // Create card
        Card card = new Card();
        card.setCardKey(cardKey);
        card.setEncryptedKey(encryptedKey);
        card.setStatus(0); // unused
        card.setCreateTime(LocalDateTime.now());
        card.setCardType(type);
        card.setDuration(duration != null ? duration : 0);
        card.setTotalCount(totalCount != null ? totalCount : 0);
        card.setRemainingCount(totalCount != null ? totalCount : 0);
        card.setCreatorType(creator);
        card.setCreatorId(creatorId);
        card.setCreatorName(creatorName);
        card.setApiKeyId(apiKeyId);
        if (verifyMethod != null) {
            try {
                card.setVerifyMethod(Card.VerifyMethod.valueOf(verifyMethod));
            } catch (IllegalArgumentException e) {
                throw new BusinessException("无效的验证方式: " + verifyMethod);
            }
        }
        card.setEncryptionType("advanced");

        return cardRepository.save(card);
    }

    /**
     * 验证卡密。
     * <p>校验卡密是否存在、是否停用、机器码是否匹配，并根据卡密类型检查有效期或扣减次数。
     * 首次验证时自动绑定机器码。</p>
     *
     * @param cardKey    卡密明文
     * @param machineCode 机器码（可为 null）
     * @param apiKeyId   API Key ID（用于触发 Webhook 回调）
     * @return 验证结果 Map，包含 success、message、statusCode 等字段
     */
    @Transactional
    public Map<String, Object> verifyCard(String cardKey, String machineCode, Long apiKeyId) {
        Map<String, Object> result = new HashMap<>();

        Card card = cardRepository.findByCardKey(cardKey).orElse(null);
        if (card == null) {
            result.put("success", false);
            result.put("message", "卡密不存在");
            result.put("statusCode", 404);
            return result;
        }

        // Check if disabled
        if (card.getStatus() == 2) {
            result.put("success", false);
            result.put("message", "卡密已停用");
            result.put("statusCode", 402);
            return result;
        }

        // Check machine code binding
        if (card.getMachineCode() != null && !card.getMachineCode().isEmpty()) {
            if (machineCode == null || !card.getMachineCode().equals(machineCode)) {
                result.put("success", false);
                result.put("message", "机器码不匹配");
                result.put("statusCode", 403);
                return result;
            }
        } else if (machineCode != null && !machineCode.isEmpty()) {
            // Bind machine code on first use
            card.setMachineCode(machineCode);
        }

        // Get status
        CardStatus cardStatus = cardStatusRepository.findByCardHash(card.getEncryptedKey()).orElse(null);

        if (card.getCardType() == Card.CardType.time) {
            // Time card - check expiry
            if (cardStatus != null && cardStatus.getExpireTime() != null
                    && cardStatus.getExpireTime().isBefore(LocalDateTime.now())) {
                result.put("success", false);
                result.put("message", "卡密已过期");
                result.put("statusCode", 401);
                return result;
            }
            // Calculate remaining time
            if (cardStatus != null && cardStatus.getExpireTime() != null) {
                long remainingSeconds = java.time.Duration.between(LocalDateTime.now(), cardStatus.getExpireTime()).getSeconds();
                result.put("remaining_time", remainingSeconds);
                result.put("expire_time", cardStatus.getExpireTime().toString());
            }
        } else if (card.getCardType() == Card.CardType.count) {
            // Count card - check remaining count
            if (cardStatus != null && cardStatus.getRemainCount() <= 0) {
                result.put("success", false);
                result.put("message", "次数已用尽");
                result.put("statusCode", 403);
                return result;
            }
            // Decrement count
            if (cardStatus != null) {
                cardStatus.setRemainCount(cardStatus.getRemainCount() - 1);
                cardStatus.setLastUseTime(LocalDateTime.now());
                cardStatusRepository.save(cardStatus);
                result.put("remaining_count", cardStatus.getRemainCount());
            }
        }

        // Update card status
        if (card.getStatus() == 0) {
            card.setStatus(1); // mark as used
            card.setUseTime(LocalDateTime.now());
        }
        cardRepository.save(card);

        result.put("success", true);
        result.put("message", "验证成功");
        result.put("statusCode", 200);

        // Trigger webhook
        if (apiKeyId != null) {
            try {
                webhookService.triggerWebhook(apiKeyId, card, "verify");
            } catch (Exception e) {
                // Don't fail verification on webhook error
            }
        }

        return result;
    }

    /**
     * 根据卡密明文查询卡密。
     *
     * @param cardKey 卡密明文
     * @return 卡密实体，不存在时返回 null
     */
    public Card getCardByKey(String cardKey) {
        return cardRepository.findByCardKey(cardKey).orElse(null);
    }

    /**
     * 分页查询指定创建者的卡密列表。
     *
     * @param creatorType 创建者类型
     * @param creatorId   创建者 ID
     * @param pageable    分页参数
     * @return 卡密分页结果
     */
    public Page<Card> getCardsByCreator(String creatorType, Integer creatorId, Pageable pageable) {
        Card.CreatorType type;
        try {
            type = Card.CreatorType.valueOf(creatorType);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("无效的创建者类型: " + creatorType);
        }
        return cardRepository.findByCreatorTypeAndCreatorId(type, creatorId, pageable);
    }

    /**
     * 查询指定创建者的全部卡密列表（不分页）。
     *
     * @param creatorType 创建者类型
     * @param creatorId   创建者 ID
     * @return 卡密列表
     */
    public List<Card> getCardsByCreator(String creatorType, Integer creatorId) {
        Card.CreatorType type;
        try {
            type = Card.CreatorType.valueOf(creatorType);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("无效的创建者类型: " + creatorType);
        }
        return cardRepository.findByCreatorTypeAndCreatorId(type, creatorId);
    }

    /**
     * 停用卡密。
     *
     * @param cardId 卡密 ID
     */
    @Transactional
    public void disableCard(Integer cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new BusinessException("卡密不存在"));
        card.setStatus(2);
        cardRepository.save(card);
    }

    /**
     * 启用卡密（恢复为未使用状态）。
     *
     * @param cardId 卡密 ID
     */
    @Transactional
    public void enableCard(Integer cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new BusinessException("卡密不存在"));
        card.setStatus(0);
        cardRepository.save(card);
    }

    /**
     * 解绑卡密的机器码。
     *
     * @param cardId 卡密 ID
     */
    @Transactional
    public void unbindMachineCode(Integer cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new BusinessException("卡密不存在"));
        card.setMachineCode(null);
        cardRepository.save(card);
    }

    /**
     * 获取卡密统计信息。
     * <p>包含总数、各状态数量、各类型数量、今日新增数量。</p>
     *
     * @return 统计数据 Map
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCards", cardRepository.count());
        stats.put("unusedCards", cardRepository.countByStatus(0));
        stats.put("usedCards", cardRepository.countByStatus(1));
        stats.put("disabledCards", cardRepository.countByStatus(2));
        stats.put("timeCards", cardRepository.countByCardType(Card.CardType.time));
        stats.put("countCards", cardRepository.countByCardType(Card.CardType.count));
        stats.put("todayCards", cardRepository.countByCreateTimeAfter(LocalDateTime.now().toLocalDate().atStartOfDay()));
        return stats;
    }

    /**
     * 删除卡密及其关联的加密数据和状态记录。
     *
     * @param cardId 卡密 ID
     */
    @Transactional
    public void deleteCard(Integer cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new BusinessException("卡密不存在"));
        // Delete cipher and status first
        cardCipherRepository.findByCardHash(card.getEncryptedKey())
                .ifPresent(c -> cardCipherRepository.delete(c));
        cardStatusRepository.findByCardHash(card.getEncryptedKey())
                .ifPresent(s -> cardStatusRepository.delete(s));
        cardRepository.delete(card);
    }
}
