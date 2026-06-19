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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.security.MessageDigest;

/**
 * 卡密业务服务。
 * <p>提供卡密的生成、验证、绑定/解绑机器码、启停用、删除及统计等功能。
 * 卡密分为时长卡和次数卡两种类型，支持 AES-GCM 加密存储和 HMAC 签名校验。</p>
 */
@Service
public class CardService {

    private static final Logger log = LoggerFactory.getLogger(CardService.class);
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
     */
    @Transactional
    public Card generateCard(String cardType, Integer duration, Integer totalCount,
                             String creatorType, Integer creatorId, String creatorName,
                             String verifyMethod, Integer days, Integer apiKeyId) {
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

        try {
            String cardKey = obfuscator.generateCardKey();
            String encryptedKey = obfuscator.generateEncryptedKey(cardKey);

            // 创建加密数据：cipherData 仅用于存储，aesKey 不持久化（无需解密）
            // 签名使用持久化的 salt 作为 HMAC 密钥派生源，保证后续可验证
            String aesKey = cryptoUtil.generateAesKey();
            String iv = cryptoUtil.generateIv();
            String cipherData = cryptoUtil.encrypt(cardKey, aesKey, iv);
            String signData = cryptoUtil.hmacSignWithStringKey(cardKey, "global");
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
        } catch (BusinessException e) {
            throw e; // 业务异常直接抛出
        } catch (Exception e) {
            // 安全修复：捕获具体异常并转换为 BusinessException，移除 throws Exception
            log.error("卡密生成失败: {}", e.getMessage(), e);
            throw new BusinessException("卡密生成失败，请联系管理员");
        }
    }

    /**
     * 批量生成卡密（减少数据库往返次数）。
     * <p>使用 saveAll 批量插入卡密、加密数据和状态记录，替代循环内逐个 save。</p>
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
     * @param count        生成数量
     * @return 生成的卡密列表
     */
    @Transactional
    public List<Card> generateCardsBatch(String cardType, Integer duration, Integer totalCount,
                                          String creatorType, Integer creatorId, String creatorName,
                                          String verifyMethod, Integer days, Integer apiKeyId, int count) {
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

        List<Card> cards = new ArrayList<>(count);
        List<CardCipher> ciphers = new ArrayList<>(count);
        List<CardStatus> statuses = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            try {
                String cardKey = obfuscator.generateCardKey();
                String encryptedKey = obfuscator.generateEncryptedKey(cardKey);

                String aesKey = cryptoUtil.generateAesKey();
                String iv = cryptoUtil.generateIv();
                String cipherData = cryptoUtil.encrypt(cardKey, aesKey, iv);
                String signData = cryptoUtil.hmacSignWithStringKey(cardKey, "global");

                CardCipher cipher = new CardCipher();
                cipher.setCardHash(encryptedKey);
                cipher.setCipherData(cipherData);
                cipher.setSignData(signData);
                cipher.setSalt("global");
                cipher.setIv(iv);
                ciphers.add(cipher);

                CardStatus status = new CardStatus();
                status.setCardHash(encryptedKey);
                status.setIsValid(true);
                if ("time".equals(cardType) && days != null) {
                    status.setExpireTime(LocalDateTime.now().plusDays(days));
                }
                if ("count".equals(cardType) && totalCount != null) {
                    status.setTotalCount(totalCount);
                    status.setRemainCount(totalCount);
                }
                statuses.add(status);

                Card card = new Card();
                card.setCardKey(cardKey);
                card.setEncryptedKey(encryptedKey);
                card.setStatus(0);
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
                cards.add(card);
            } catch (Exception e) {
                log.error("批量生成卡密失败: {}", e.getMessage(), e);
                throw new BusinessException("卡密生成失败，请联系管理员");
            }
        }

        // 批量插入，减少数据库往返次数
        cardCipherRepository.saveAll(ciphers);
        cardStatusRepository.saveAll(statuses);
        return cardRepository.saveAll(cards);
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
    public Map<String, Object> verifyCard(String cardKey, String machineCode, Integer apiKeyId) {
        Map<String, Object> result = new HashMap<>();

        // 1. 查询卡密（悲观锁）
        Card card = cardRepository.findByCardKeyForUpdate(cardKey).orElse(null);
        if (card == null || card.getStatus() == 2) {
            return buildErrorResponse("卡密无效", 400);
        }

        // 2. 校验 CardStatus 有效性
        CardStatus cardStatus = cardStatusRepository.findByCardHashForUpdate(card.getEncryptedKey()).orElse(null);
        if (cardStatus != null && !Boolean.TRUE.equals(cardStatus.getIsValid())) {
            return buildErrorResponse("卡密无效", 400);
        }

        // 3. 校验 CardCipher 签名（防止卡密被篡改）
        String cipherError = validateCardCipher(card);
        if (cipherError != null) {
            return buildErrorResponse(cipherError, 400);
        }

        // 4. 校验是否允许重复验证（allowReverify=false 且卡密已使用时拒绝）
        if (Boolean.FALSE.equals(card.getAllowReverify()) && card.getStatus() == 1) {
            return buildErrorResponse("此卡密不允许重复验证", 403);
        }

        // 5. 校验/绑定机器码
        String machineCodeError = validateOrBindMachineCode(card, machineCode);
        if (machineCodeError != null) {
            return buildErrorResponse(machineCodeError, 400);
        }

        // 6. 按卡密类型验证
        if (card.getCardType() == Card.CardType.time) {
            String timeError = verifyTimeCard(cardStatus, result);
            if (timeError != null) {
                return buildErrorResponse(timeError, 400);
            }
        } else if (card.getCardType() == Card.CardType.count) {
            String countError = verifyCountCard(card, cardStatus, result);
            if (countError != null) {
                return buildErrorResponse(countError, 403);
            }
        }

        // 7. 更新卡密使用状态
        finalizeCardUsage(card);

        result.put("success", true);
        result.put("message", "验证成功");
        result.put("statusCode", 200);

        // 8. 触发 Webhook
        triggerWebhookSafely(apiKeyId, card);

        return result;
    }

    /**
     * 校验 CardCipher 签名是否匹配，防止卡密数据被篡改。
     * <p>注意：由于历史原因 aesKey 未持久化，此处使用 salt 作为 HMAC 密钥派生源进行签名校验。
     * 生成卡密时也使用相同方式签名，保证一致性。</p>
     * @param card 卡密实体
     * @return 错误消息（无错误返回 null）
     */
    private String validateCardCipher(Card card) {
        CardCipher cipher = cardCipherRepository.findByCardHash(card.getEncryptedKey()).orElse(null);
        if (cipher == null) {
            return "卡密加密数据缺失";
        }
        // 使用持久化的 salt 重新计算签名并比对（常量时间比较防止时序攻击）
        try {
            String expectedSign = cryptoUtil.hmacSignWithStringKey(card.getCardKey(), cipher.getSalt());
            if (expectedSign == null || !MessageDigest.isEqual(
                    expectedSign.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    cipher.getSignData().getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
                return "卡密签名校验失败";
            }
        } catch (Exception e) {
            log.warn("卡密签名计算失败 cardId={}: {}", card.getId(), e.getMessage());
            return "卡密签名校验失败";
        }
        return null;
    }

    /** 构建错误响应 */
    private Map<String, Object> buildErrorResponse(String message, int statusCode) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("message", message);
        result.put("statusCode", statusCode);
        return result;
    }

    /** 校验或绑定机器码，返回错误消息（无错误返回 null） */
    private String validateOrBindMachineCode(Card card, String machineCode) {
        if (card.getMachineCode() != null && !card.getMachineCode().isEmpty()) {
            if (machineCode == null || !card.getMachineCode().equals(machineCode)) {
                return "卡密无效或机器码不匹配";
            }
        } else if (machineCode != null && !machineCode.isEmpty()) {
            card.setMachineCode(machineCode);
        }
        return null;
    }

    /** 时长卡验证，返回错误消息（无错误返回 null，结果写入 result） */
    private String verifyTimeCard(CardStatus cardStatus, Map<String, Object> result) {
        if (cardStatus != null && cardStatus.getExpireTime() != null
                && cardStatus.getExpireTime().isBefore(LocalDateTime.now())) {
            return "卡密无效";
        }
        if (cardStatus != null && cardStatus.getExpireTime() != null) {
            long remainingSeconds = java.time.Duration.between(LocalDateTime.now(), cardStatus.getExpireTime()).getSeconds();
            result.put("remaining_time", remainingSeconds);
            result.put("expire_time", cardStatus.getExpireTime().toString());
        }
        return null;
    }

    /** 次数卡验证（悲观锁防并发），返回错误消息（无错误返回 null） */
    private String verifyCountCard(Card card, CardStatus cardStatus, Map<String, Object> result) {
        CardStatus lockedStatus = cardStatus;
        if (lockedStatus == null) {
            lockedStatus = cardStatusRepository.findByCardHashForUpdate(card.getEncryptedKey()).orElse(null);
        }
        if (lockedStatus == null || lockedStatus.getRemainCount() == null || lockedStatus.getRemainCount() <= 0) {
            return "次数已用尽";
        }
        lockedStatus.setRemainCount(lockedStatus.getRemainCount() - 1);
        lockedStatus.setLastUseTime(LocalDateTime.now());
        cardStatusRepository.save(lockedStatus);
        // 同步更新 Card.remainingCount，保持主表与状态表一致
        card.setRemainingCount(lockedStatus.getRemainCount());
        result.put("remaining_count", lockedStatus.getRemainCount());
        return null;
    }

    /** 更新卡密首次使用状态 */
    private void finalizeCardUsage(Card card) {
        if (card.getStatus() == 0) {
            card.setStatus(1);
            card.setUseTime(LocalDateTime.now());
        }
        cardRepository.save(card);
    }

    /** 安全触发 Webhook（失败不阻塞主流程） */
    private void triggerWebhookSafely(Integer apiKeyId, Card card) {
        if (apiKeyId != null) {
            try {
                webhookService.triggerWebhook(apiKeyId, card, "verify");
            } catch (Exception e) {
                log.warn("Webhook trigger failed for card {} apiKeyId={}: {}", card.getId(), apiKeyId, e.getMessage(), e);
            }
        }
    }

    /**
     * 根据卡密明文查询卡密。
     *
     * @param cardKey 卡密明文
     * @return 卡密实体，不存在时返回 null
     */
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
    public Page<Card> getCardsByCreator(String creatorType, Integer creatorId, Pageable pageable) {
        Card.CreatorType type;
        try {
            type = Card.CreatorType.valueOf(creatorType);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("无效的创建者类型: " + creatorType);
        }
        if (creatorId == null) {
            return cardRepository.findAll(pageable);
        }
        return cardRepository.findByCreatorTypeAndCreatorId(type, creatorId, pageable);
    }

    /**
     * 查询指定创建者的全部卡密列表（不分页）。
     * 当 creatorId 为 null 时，按创建者类型查询所有卡密。
     *
     * @param creatorType 创建者类型
     * @param creatorId   创建者 ID（可为 null）
     * @return 卡密列表
     */
    @Transactional(readOnly = true)
    public List<Card> getCardsByCreator(String creatorType, Integer creatorId) {
        Card.CreatorType type;
        try {
            type = Card.CreatorType.valueOf(creatorType);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("无效的创建者类型: " + creatorType);
        }
        if (creatorId == null) {
            return cardRepository.findByCreatorType(type);
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
        cardStatusRepository.findByCardHash(card.getEncryptedKey())
                .ifPresent(status -> {
                    status.setIsValid(false);
                    cardStatusRepository.save(status);
                });
    }

    /**
     * 启用卡密（恢复为之前的状态）。
     * <p>如果卡密之前是已使用状态(1)，恢复后仍为已使用；
     * 如果之前是未使用状态(0)，恢复后仍为未使用。</p>
     *
     * @param cardId 卡密 ID
     */
    @Transactional
    public void enableCard(Integer cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new BusinessException("卡密不存在"));
        // 恢复到暂停前的状态：有使用记录则恢复为已使用，否则恢复为未使用
        card.setStatus(card.getUseTime() != null ? 1 : 0);
        cardRepository.save(card);
        cardStatusRepository.findByCardHash(card.getEncryptedKey())
                .ifPresent(status -> {
                    status.setIsValid(true);
                    cardStatusRepository.save(status);
                });
    }

    /**
     * 设置卡密为指定状态。
     *
     * @param cardId 卡密 ID
     * @param status 目标状态（0=未使用，1=已使用）
     */
    @Transactional
    public void setCardStatus(Integer cardId, int status) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new BusinessException("卡密不存在"));
        card.setStatus(status);
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
        if (card.getStatus() == 2) {
            throw new BusinessException("已停用的卡密不能解绑机器码");
        }
        card.setMachineCode(null);
        cardRepository.save(card);
    }

    /**
     * 自助解绑卡密的机器码（公开接口调用）。
     * <p>需要卡密允许自助解绑。如提供 machine_code 则验证匹配后解绑；
     * 如未提供 machine_code，则在 allowSelfUnbind 为 true 时直接解绑。
     * 使用悲观锁保证读-改-写的原子性。</p>
     *
     * @param cardKey     卡密明文
     * @param machineCode 当前绑定的机器码（可选）
     */
    @Transactional
    public void selfUnbindMachineCode(String cardKey, String machineCode) {
        Card card = cardRepository.findByCardKeyForUpdate(cardKey).orElse(null);
        // 统一错误消息，防止通过不同响应枚举有效卡密
        if (card == null || card.getStatus() == 2) {
            throw new BusinessException("卡密无效或已停用");
        }
        if (!card.getAllowSelfUnbind()) {
            throw new BusinessException("此卡密不支持自助解绑");
        }
        if (card.getMachineCode() == null || card.getMachineCode().isEmpty()) {
            throw new BusinessException("该卡密未绑定机器码，无需解绑");
        }
        // 如果提供了机器码，验证是否匹配
        if (machineCode != null && !machineCode.isBlank()) {
            if (!card.getMachineCode().equals(machineCode)) {
                throw new BusinessException("机器码不匹配");
            }
        }
        card.setMachineCode(null);
        cardRepository.save(card);
    }

    /**
     * 获取卡密统计信息。
     * <p>包含总数、各状态数量、各类型数量、今日新增数量。</p>
     *
     * @return 统计数据 Map
     */
    @Transactional(readOnly = true)
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
     * 根据订单信息批量生成卡密（支付回调时调用）。
     * <p>根据订单的卡密类型、数量、规格等信息批量生成卡密，
     * 使用 saveAll() 批量插入替代逐条 save()，减少数据库往返次数。</p>
     *
     * @param order 订单实体
     * @return 生成的卡密明文（多个以逗号分隔）
     */
    @Transactional
    public String generateCardsForOrder(Order order) {
        int quantity = order.getQuantity() != null ? order.getQuantity() : 1;

        // 预解析卡密规格，避免循环内重复解析
        Integer duration = null;
        Integer totalCount = null;
        Integer days = null;
        if ("time".equals(order.getCardType())) {
            days = parseDaysFromSpec(order.getCardSpec());
            duration = days * 24 * 60;
        } else if ("count".equals(order.getCardType())) {
            totalCount = parseCountFromSpec(order.getCardSpec());
        }

        // 收集所有待保存实体
        List<Card> cards = new ArrayList<>(quantity);
        List<CardCipher> ciphers = new ArrayList<>(quantity);
        List<CardStatus> statuses = new ArrayList<>(quantity);
        List<String> cardKeys = new ArrayList<>(quantity);

        for (int i = 0; i < quantity; i++) {
            try {
                String cardKey = obfuscator.generateCardKey();
                String encryptedKey = obfuscator.generateEncryptedKey(cardKey);

                // 创建加密数据
                String aesKey = cryptoUtil.generateAesKey();
                String iv = cryptoUtil.generateIv();
                String cipherData = cryptoUtil.encrypt(cardKey, aesKey, iv);
                String signData = cryptoUtil.hmacSignWithStringKey(cardKey, "global");

                CardCipher cipher = new CardCipher();
                cipher.setCardHash(encryptedKey);
                cipher.setCipherData(cipherData);
                cipher.setSignData(signData);
                cipher.setSalt("global");
                cipher.setIv(iv);
                ciphers.add(cipher);

                // 创建卡密状态
                CardStatus status = new CardStatus();
                status.setCardHash(encryptedKey);
                status.setIsValid(true);
                if ("time".equals(order.getCardType()) && days != null) {
                    status.setExpireTime(LocalDateTime.now().plusDays(days));
                }
                if ("count".equals(order.getCardType()) && totalCount != null) {
                    status.setTotalCount(totalCount);
                    status.setRemainCount(totalCount);
                }
                statuses.add(status);

                // 创建卡密主记录
                Card card = new Card();
                card.setCardKey(cardKey);
                card.setEncryptedKey(encryptedKey);
                card.setStatus(0);
                card.setCreateTime(LocalDateTime.now());
                card.setCardType(Card.CardType.valueOf(order.getCardType()));
                card.setDuration(duration != null ? duration : 0);
                card.setTotalCount(totalCount != null ? totalCount : 0);
                card.setRemainingCount(totalCount != null ? totalCount : 0);
                card.setCreatorType(Card.CreatorType.system);
                card.setCreatorId(order.getUserId());
                card.setCreatorName(order.getUsername() != null ? order.getUsername() : "system");
                card.setApiKeyId(null);
                card.setVerifyMethod(Card.VerifyMethod.web);
                card.setEncryptionType("advanced");
                cards.add(card);

                cardKeys.add(cardKey);
            } catch (Exception e) {
                log.error("为订单 {} 生成卡密失败: {}", order.getOrderNo(), e.getMessage(), e);
                throw new BusinessException("卡密生成失败，请联系管理员");
            }
        }

        // 批量插入，减少数据库往返次数
        cardCipherRepository.saveAll(ciphers);
        cardStatusRepository.saveAll(statuses);
        cardRepository.saveAll(cards);

        return String.join(",", cardKeys);
    }

    /**
     * 从卡密规格中解析天数
     * @param spec 规格字符串，如 "7天"、"30天"
     * @return 天数，默认7天
     */
    private Integer parseDaysFromSpec(String spec) {
        if (spec == null || spec.isBlank()) {
            log.warn("parseDaysFromSpec: spec 为空，使用默认值 7 天");
            return 7;
        }
        try {
            String num = spec.replaceAll("[^0-9]", "");
            if (num.isEmpty()) {
                log.warn("parseDaysFromSpec: spec={} 无法解析出数字，使用默认值 7 天", spec);
                return 7;
            }
            return Integer.parseInt(num);
        } catch (NumberFormatException e) {
            log.warn("parseDaysFromSpec: spec={} 解析失败，使用默认值 7 天", spec);
            return 7;
        }
    }

    /**
     * 从卡密规格中解析次数
     * @param spec 规格字符串，如 "100次"
     * @return 次数，默认100次
     */
    private Integer parseCountFromSpec(String spec) {
        if (spec == null || spec.isBlank()) {
            log.warn("parseCountFromSpec: spec 为空，使用默认值 100 次");
            return 100;
        }
        try {
            String num = spec.replaceAll("[^0-9]", "");
            if (num.isEmpty()) {
                log.warn("parseCountFromSpec: spec={} 无法解析出数字，使用默认值 100 次", spec);
                return 100;
            }
            return Integer.parseInt(num);
        } catch (NumberFormatException e) {
            log.warn("parseCountFromSpec: spec={} 解析失败，使用默认值 100 次", spec);
            return 100;
        }
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

    /**
     * 更新卡密信息。
     * <p>允许更新卡密的类型、时长、总次数、剩余次数、验证方式、是否允许自助解绑等字段。
     * 不允许更新 cardKey、encryptedKey、status 等核心安全字段。</p>
     *
     * @param cardId 卡密 ID
     * @param updates 需要更新的字段 Map
     * @return 更新后的卡密实体
     */
    @Transactional
    public Card updateCard(Integer cardId, Map<String, Object> updates) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new BusinessException("卡密不存在"));
        boolean needSyncStatus = false;
        if (updates.containsKey("cardType")) {
            try {
                card.setCardType(Card.CardType.valueOf(updates.get("cardType").toString()));
            } catch (IllegalArgumentException e) {
                throw new BusinessException("无效的卡密类型: " + updates.get("cardType"));
            }
        }
        if (updates.containsKey("duration")) {
            try {
                card.setDuration(((Number) updates.get("duration")).intValue());
            } catch (ClassCastException | NullPointerException e) {
                throw new BusinessException("duration 字段格式无效");
            }
        }
        if (updates.containsKey("totalCount")) {
            try {
                card.setTotalCount(((Number) updates.get("totalCount")).intValue());
                needSyncStatus = true;
            } catch (ClassCastException | NullPointerException e) {
                throw new BusinessException("totalCount 字段格式无效");
            }
        }
        if (updates.containsKey("remainingCount")) {
            try {
                card.setRemainingCount(((Number) updates.get("remainingCount")).intValue());
                needSyncStatus = true;
            } catch (ClassCastException | NullPointerException e) {
                throw new BusinessException("remainingCount 字段格式无效");
            }
        }
        if (updates.containsKey("verifyMethod")) {
            try {
                card.setVerifyMethod(Card.VerifyMethod.valueOf(updates.get("verifyMethod").toString()));
            } catch (IllegalArgumentException e) {
                throw new BusinessException("无效的验证方式: " + updates.get("verifyMethod"));
            }
        }
        if (updates.containsKey("allowSelfUnbind")) {
            card.setAllowSelfUnbind(Boolean.parseBoolean(updates.get("allowSelfUnbind").toString()));
        }
        if (updates.containsKey("stackTimeIfSameMachine")) {
            card.setStackTimeIfSameMachine(Boolean.parseBoolean(updates.get("stackTimeIfSameMachine").toString()));
        }
        if (updates.containsKey("allowReverify")) {
            card.setAllowReverify(Boolean.parseBoolean(updates.get("allowReverify").toString()));
        }
        Card saved = cardRepository.save(card);
        // 同步更新 CardStatus 表对应字段，保持主表与状态表一致
        if (needSyncStatus) {
            cardStatusRepository.findByCardHash(card.getEncryptedKey()).ifPresent(status -> {
                if (card.getTotalCount() != null) {
                    status.setTotalCount(card.getTotalCount());
                }
                if (card.getRemainingCount() != null) {
                    status.setRemainCount(card.getRemainingCount());
                }
                cardStatusRepository.save(status);
            });
        }
        return saved;
    }

    /**
     * 根据 API Key ID 获取关联的卡密列表
     * @param apiKeyId API Key ID
     * @return 卡密列表
     */
    @Transactional(readOnly = true)
    public List<Card> getCardsByApiKeyId(Integer apiKeyId) {
        return cardRepository.findByApiKeyId(apiKeyId);
    }

    /**
     * 获取卡密使用趋势数据
     * @param days 统计天数
     * @return 包含每日使用数量的趋势数据
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getUsageTrend(int days) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> trend = new ArrayList<>();
        LocalDateTime startTime = LocalDateTime.now().minusDays(days);
        List<Object[]> rows = cardRepository.countUsedCardsGroupByDay(startTime);
        for (Object[] row : rows) {
            Map<String, Object> item = new HashMap<>();
            item.put("date", row[0] != null ? row[0].toString() : "");
            item.put("count", row[1] != null ? ((Number) row[1]).longValue() : 0);
            trend.add(item);
        }
        result.put("trend", trend);
        return result;
    }
}
