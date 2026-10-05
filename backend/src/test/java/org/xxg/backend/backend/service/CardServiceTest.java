package org.xxg.backend.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.xxg.backend.backend.entity.Card;
import org.xxg.backend.backend.entity.CardCipher;
import org.xxg.backend.backend.entity.CardStatus;
import org.xxg.backend.backend.entity.Order;
import org.xxg.backend.backend.exception.BusinessException;
import org.xxg.backend.backend.mapper.*;
import org.xxg.backend.backend.util.AdvancedCryptoUtil;
import org.xxg.backend.backend.util.CustomCardObfuscator;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * CardService 单元测试
 * 测试卡密生成、验证、启停用等业务逻辑
 */
@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock
    private CardRepository cardRepository;
    @Mock
    private CardCipherRepository cardCipherRepository;
    @Mock
    private CardStatusRepository cardStatusRepository;
    @Mock
    private ApiKeyRepository apiKeyRepository;
    @Mock
    private AdvancedCryptoUtil cryptoUtil;
    @Mock
    private CustomCardObfuscator obfuscator;
    @Mock
    private WebhookService webhookService;

    @InjectMocks
    private CardService cardService;

    private Card testCard;
    private CardStatus testCardStatus;
    private CardCipher testCardCipher;

    @BeforeEach
    void setUp() throws Exception {
        // 准备测试卡密
        testCard = new Card();
        testCard.setId(1);
        testCard.setCardKey("TEST-XXXX-YYYY-ZZZZ");
        testCard.setEncryptedKey("encrypted-key-123");
        testCard.setStatus(0); // 未使用
        testCard.setCardType(Card.CardType.time);
        testCard.setMachineCode(null);
        testCard.setAllowSelfUnbind(false);

        // 准备测试卡密状态
        testCardStatus = new CardStatus();
        testCardStatus.setCardHash("encrypted-key-123");
        testCardStatus.setIsValid(true);
        testCardStatus.setExpireTime(LocalDateTime.now().plusDays(7));

        // 准备测试卡密加密数据（用于 validateCardCipher 签名校验）
        testCardCipher = new CardCipher();
        testCardCipher.setCardHash("encrypted-key-123");
        testCardCipher.setSalt("global");
        testCardCipher.setSignData("sign-data");

        // Mock 加密工具（使用 lenient 避免不必要的 stub 报错，因为不是所有测试都用得到）
        lenient().when(obfuscator.generateCardKey()).thenReturn("TEST-XXXX-YYYY-ZZZZ");
        lenient().when(obfuscator.generateEncryptedKey("TEST-XXXX-YYYY-ZZZZ")).thenReturn("encrypted-key-123");
        lenient().when(cryptoUtil.generateAesKey()).thenReturn("aes-key");
        lenient().when(cryptoUtil.generateIv()).thenReturn("iv-value");
        lenient().when(cryptoUtil.encrypt(anyString(), anyString(), anyString())).thenReturn("cipher-data");
        // 卡密签名使用每卡随机 salt（安全修复 F3），与 signData 一同落库
        lenient().when(cryptoUtil.generateSalt()).thenReturn("0123456789abcdef0123456789abcdef");
        // 安全修复后改用 hmacSignWithStringKey（字符串密钥），替代原 hmacSign
        lenient().when(cryptoUtil.hmacSignWithStringKey(anyString(), anyString())).thenReturn("sign-data");
        // validateCardCipher 签名校验需要的 mock
        lenient().when(cardCipherRepository.findByCardHash("encrypted-key-123")).thenReturn(Optional.of(testCardCipher));
    }

    @Test
    @DisplayName("生成时长卡成功")
    void generateCard_TimeCard_Success() throws Exception {
        when(cardCipherRepository.save(any(CardCipher.class))).thenReturn(new CardCipher());
        when(cardStatusRepository.save(any(CardStatus.class))).thenReturn(new CardStatus());
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);

        Card result = cardService.generateCard("time", 1440, null,
                "admin", 1, "admin", "web", 7, null);

        assertNotNull(result);
        assertEquals("TEST-XXXX-YYYY-ZZZZ", result.getCardKey());
        assertEquals(Card.CardType.time, result.getCardType());
        verify(cardCipherRepository).save(any(CardCipher.class));
        verify(cardStatusRepository).save(any(CardStatus.class));
        verify(cardRepository).save(any(Card.class));
    }

    @Test
    @DisplayName("生成次数卡成功")
    void generateCard_CountCard_Success() throws Exception {
        testCard.setCardType(Card.CardType.count);
        when(cardCipherRepository.save(any(CardCipher.class))).thenReturn(new CardCipher());
        when(cardStatusRepository.save(any(CardStatus.class))).thenReturn(new CardStatus());
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);

        Card result = cardService.generateCard("count", null, 100,
                "admin", 1, "admin", "web", null, null);

        assertNotNull(result);
        assertEquals(Card.CardType.count, result.getCardType());
    }

    @Test
    @DisplayName("生成卡密失败 - 无效的卡密类型")
    void generateCard_InvalidCardType_ThrowsException() {
        assertThrows(BusinessException.class,
                () -> cardService.generateCard("invalid", null, null,
                        "admin", 1, "admin", "web", null, null));
    }

    @Test
    @DisplayName("生成卡密失败 - 无效的创建者类型")
    void generateCard_InvalidCreatorType_ThrowsException() {
        assertThrows(BusinessException.class,
                () -> cardService.generateCard("time", null, null,
                        "invalid", 1, "admin", "web", 7, null));
    }

    @Test
    @DisplayName("生成时长卡失败 - 缺少有效天数（防止生成永不过期的卡密）")
    void generateCard_TimeCardWithoutDays_ThrowsException() {
        assertThrows(BusinessException.class,
                () -> cardService.generateCard("time", 1440, null,
                        "admin", 1, "admin", "web", null, null));
    }

    @Test
    @DisplayName("生成次数卡失败 - 缺少总次数")
    void generateCard_CountCardWithoutTotalCount_ThrowsException() {
        assertThrows(BusinessException.class,
                () -> cardService.generateCard("count", null, null,
                        "admin", 1, "admin", "web", null, null));
    }

    @Test
    @DisplayName("批量生成时长卡失败 - 缺少有效天数")
    void generateCardsBatch_TimeCardWithoutDays_ThrowsException() {
        assertThrows(BusinessException.class,
                () -> cardService.generateCardsBatch("time", 1440, null,
                        "admin", 1, "admin", "web", null, null, 5));
    }

    @Test
    @DisplayName("验证时长卡成功 - 首次使用绑定机器码")
    void verifyCard_TimeCard_FirstUse_BindsMachineCode() {
        when(cardRepository.findByCardKeyForUpdate("TEST-XXXX-YYYY-ZZZZ"))
                .thenReturn(Optional.of(testCard));
        when(cardStatusRepository.findByCardHashForUpdate("encrypted-key-123"))
                .thenReturn(Optional.of(testCardStatus));
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);

        Map<String, Object> result = cardService.verifyCard("TEST-XXXX-YYYY-ZZZZ", "machine-001", null);

        assertTrue((Boolean) result.get("success"));
        assertEquals("验证成功", result.get("message"));
        assertEquals("machine-001", testCard.getMachineCode()); // 机器码应被绑定
        assertEquals(1, testCard.getStatus()); // 状态应变为已使用
    }

    @Test
    @DisplayName("验证时长卡失败 - 机器码不匹配")
    void verifyCard_TimeCard_MachineCodeMismatch_ReturnsFail() {
        testCard.setMachineCode("machine-001");
        when(cardRepository.findByCardKeyForUpdate("TEST-XXXX-YYYY-ZZZZ"))
                .thenReturn(Optional.of(testCard));

        Map<String, Object> result = cardService.verifyCard("TEST-XXXX-YYYY-ZZZZ", "machine-002", null);

        assertFalse((Boolean) result.get("success"));
        assertEquals("卡密无效或机器码不匹配", result.get("message"));
    }

    @Test
    @DisplayName("验证卡密失败 - 卡密不存在")
    void verifyCard_CardNotFound_ReturnsFail() {
        when(cardRepository.findByCardKeyForUpdate("nonexistent")).thenReturn(Optional.empty());

        Map<String, Object> result = cardService.verifyCard("nonexistent", null, null);

        assertFalse((Boolean) result.get("success"));
        assertEquals("卡密无效", result.get("message"));
    }

    @Test
    @DisplayName("验证卡密失败 - 卡密已停用")
    void verifyCard_CardDisabled_ReturnsFail() {
        testCard.setStatus(2); // 已停用
        when(cardRepository.findByCardKeyForUpdate("TEST-XXXX-YYYY-ZZZZ"))
                .thenReturn(Optional.of(testCard));

        Map<String, Object> result = cardService.verifyCard("TEST-XXXX-YYYY-ZZZZ", null, null);

        assertFalse((Boolean) result.get("success"));
        assertEquals("卡密无效", result.get("message"));
    }

    @Test
    @DisplayName("验证时长卡失败 - 已过期")
    void verifyCard_TimeCard_Expired_ReturnsFail() {
        testCardStatus.setExpireTime(LocalDateTime.now().minusDays(1)); // 已过期
        when(cardRepository.findByCardKeyForUpdate("TEST-XXXX-YYYY-ZZZZ"))
                .thenReturn(Optional.of(testCard));
        when(cardStatusRepository.findByCardHashForUpdate("encrypted-key-123"))
                .thenReturn(Optional.of(testCardStatus));

        Map<String, Object> result = cardService.verifyCard("TEST-XXXX-YYYY-ZZZZ", null, null);

        assertFalse((Boolean) result.get("success"));
        assertEquals("卡密无效", result.get("message"));
    }

    @Test
    @DisplayName("验证失败的卡密不应绑定机器码（防止脏检查误写库）")
    void verifyCard_FailedVerification_DoesNotBindMachineCode() {
        // 卡密已过期：验证必然失败
        testCardStatus.setExpireTime(LocalDateTime.now().minusDays(1));
        when(cardRepository.findByCardKeyForUpdate("TEST-XXXX-YYYY-ZZZZ"))
                .thenReturn(Optional.of(testCard));
        when(cardStatusRepository.findByCardHashForUpdate("encrypted-key-123"))
                .thenReturn(Optional.of(testCardStatus));

        Map<String, Object> result = cardService.verifyCard("TEST-XXXX-YYYY-ZZZZ", "attacker-machine", null);

        assertFalse((Boolean) result.get("success"));
        // 关键断言：验证失败时机器码不得被写入实体（否则 JPA 脏检查会在提交时落库）
        assertNull(testCard.getMachineCode());
        verify(cardRepository, never()).save(any(Card.class));
    }

    @Test
    @DisplayName("验证次数卡成功 - 扣减剩余次数")
    void verifyCard_CountCard_Success_DecrementsCount() {
        testCard.setCardType(Card.CardType.count);
        testCardStatus.setTotalCount(100);
        testCardStatus.setRemainCount(50);
        testCardStatus.setExpireTime(null);

        when(cardRepository.findByCardKeyForUpdate("TEST-XXXX-YYYY-ZZZZ"))
                .thenReturn(Optional.of(testCard));
        when(cardStatusRepository.findByCardHashForUpdate("encrypted-key-123"))
                .thenReturn(Optional.of(testCardStatus));
        when(cardStatusRepository.save(any(CardStatus.class))).thenReturn(testCardStatus);
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);

        Map<String, Object> result = cardService.verifyCard("TEST-XXXX-YYYY-ZZZZ", "machine-001", null);

        assertTrue((Boolean) result.get("success"));
        assertEquals(49, testCardStatus.getRemainCount()); // 次数应减 1
    }

    @Test
    @DisplayName("验证次数卡失败 - 次数已用尽")
    void verifyCard_CountCard_NoRemaining_ReturnsFail() {
        testCard.setCardType(Card.CardType.count);
        testCardStatus.setTotalCount(100);
        testCardStatus.setRemainCount(0);

        when(cardRepository.findByCardKeyForUpdate("TEST-XXXX-YYYY-ZZZZ"))
                .thenReturn(Optional.of(testCard));
        when(cardStatusRepository.findByCardHashForUpdate("encrypted-key-123"))
                .thenReturn(Optional.of(testCardStatus));

        Map<String, Object> result = cardService.verifyCard("TEST-XXXX-YYYY-ZZZZ", "machine-001", null);

        assertFalse((Boolean) result.get("success"));
        assertEquals("次数已用尽", result.get("message"));
    }

    @Test
    @DisplayName("停用卡密成功")
    void disableCard_Success() {
        when(cardRepository.findById(1)).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);
        when(cardStatusRepository.findByCardHash("encrypted-key-123"))
                .thenReturn(Optional.of(testCardStatus));

        cardService.disableCard(1);

        assertEquals(2, testCard.getStatus());
        assertFalse(testCardStatus.getIsValid());
    }

    @Test
    @DisplayName("启用卡密成功")
    void enableCard_Success() {
        testCard.setStatus(2); // 已停用
        testCard.setUseTime(LocalDateTime.now()); // 有使用记录
        when(cardRepository.findById(1)).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);
        when(cardStatusRepository.findByCardHash("encrypted-key-123"))
                .thenReturn(Optional.of(testCardStatus));

        cardService.enableCard(1);

        assertEquals(1, testCard.getStatus()); // 恢复为已使用
        assertTrue(testCardStatus.getIsValid());
    }

    @Test
    @DisplayName("解绑机器码成功")
    void unbindMachineCode_Success() {
        testCard.setMachineCode("machine-001");
        when(cardRepository.findById(1)).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);

        cardService.unbindMachineCode(1);

        assertNull(testCard.getMachineCode());
    }

    @Test
    @DisplayName("解绑机器码失败 - 已停用的卡密不能解绑")
    void unbindMachineCode_DisabledCard_ThrowsException() {
        testCard.setStatus(2); // 已停用
        when(cardRepository.findById(1)).thenReturn(Optional.of(testCard));

        assertThrows(BusinessException.class, () -> cardService.unbindMachineCode(1));
    }

    @Test
    @DisplayName("删除卡密成功")
    void deleteCard_Success() {
        when(cardRepository.findById(1)).thenReturn(Optional.of(testCard));
        when(cardCipherRepository.findByCardHash("encrypted-key-123"))
                .thenReturn(Optional.of(new CardCipher()));
        when(cardStatusRepository.findByCardHash("encrypted-key-123"))
                .thenReturn(Optional.of(new CardStatus()));

        cardService.deleteCard(1);

        verify(cardCipherRepository).delete(any(CardCipher.class));
        verify(cardStatusRepository).delete(any(CardStatus.class));
        verify(cardRepository).delete(testCard);
    }

    @Test
    @DisplayName("自助解绑成功")
    void selfUnbindMachineCode_Success() {
        testCard.setAllowSelfUnbind(true);
        testCard.setMachineCode("machine-001");
        when(cardRepository.findByCardKeyForUpdate("TEST-XXXX-YYYY-ZZZZ"))
                .thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);

        assertDoesNotThrow(() -> cardService.selfUnbindMachineCode("TEST-XXXX-YYYY-ZZZZ", "machine-001"));
        assertNull(testCard.getMachineCode());
    }

    @Test
    @DisplayName("自助解绑失败 - 不允许自助解绑")
    void selfUnbindMachineCode_NotAllowed_ThrowsException() {
        testCard.setAllowSelfUnbind(false);
        testCard.setMachineCode("machine-001");
        when(cardRepository.findByCardKeyForUpdate("TEST-XXXX-YYYY-ZZZZ"))
                .thenReturn(Optional.of(testCard));

        assertThrows(BusinessException.class,
                () -> cardService.selfUnbindMachineCode("TEST-XXXX-YYYY-ZZZZ", "machine-001"));
    }

    @Test
    @DisplayName("自助解绑失败 - 机器码不匹配")
    void selfUnbindMachineCode_MachineCodeMismatch_ThrowsException() {
        testCard.setAllowSelfUnbind(true);
        testCard.setMachineCode("machine-001");
        when(cardRepository.findByCardKeyForUpdate("TEST-XXXX-YYYY-ZZZZ"))
                .thenReturn(Optional.of(testCard));

        assertThrows(BusinessException.class,
                () -> cardService.selfUnbindMachineCode("TEST-XXXX-YYYY-ZZZZ", "machine-002"));
    }

    @Test
    @DisplayName("获取卡密统计信息成功 - 单聚合查询")
    void getStats_Success() {
        when(cardRepository.aggregateStats(any(LocalDateTime.class), eq(Card.CardType.time), eq(Card.CardType.count)))
                .thenReturn(new Object[]{100L, 60L, 30L, 10L, 70L, 30L, 5L});

        Map<String, Object> stats = cardService.getStats();

        assertEquals(100L, stats.get("totalCards"));
        assertEquals(60L, stats.get("unusedCards"));
        assertEquals(30L, stats.get("usedCards"));
        assertEquals(10L, stats.get("disabledCards"));
        assertEquals(70L, stats.get("timeCards"));
        assertEquals(30L, stats.get("countCards"));
        assertEquals(5L, stats.get("todayCards"));
    }

    @Test
    @DisplayName("时长卡 CardStatus 缺失 - 回退 Card.expireTime 判定已过期")
    void verifyCard_TimeCard_MissingStatus_FallsBackToCardExpireTime() {
        // 回归覆盖：历史上 CardStatus 记录缺失时 verifyTimeCard 直接放行且无过期兜底
        testCard.setExpireTime(LocalDateTime.now().minusDays(1));
        when(cardRepository.findByCardKeyForUpdate("TEST-XXXX-YYYY-ZZZZ"))
                .thenReturn(Optional.of(testCard));
        when(cardStatusRepository.findByCardHashForUpdate("encrypted-key-123"))
                .thenReturn(Optional.empty());

        Map<String, Object> result = cardService.verifyCard("TEST-XXXX-YYYY-ZZZZ", "machine-001", null);

        assertFalse((Boolean) result.get("success"));
        assertEquals("卡密无效", result.get("message"));
        verify(cardRepository, never()).save(any(Card.class));
    }

    @Test
    @DisplayName("时长卡 CardStatus 缺失且无过期时间 - 保持向后兼容验证通过")
    void verifyCard_TimeCard_MissingStatus_NoExpireTime_StillPasses() {
        testCard.setExpireTime(null);
        when(cardRepository.findByCardKeyForUpdate("TEST-XXXX-YYYY-ZZZZ"))
                .thenReturn(Optional.of(testCard));
        when(cardStatusRepository.findByCardHashForUpdate("encrypted-key-123"))
                .thenReturn(Optional.empty());
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);

        Map<String, Object> result = cardService.verifyCard("TEST-XXXX-YYYY-ZZZZ", "machine-001", null);

        assertTrue((Boolean) result.get("success"));
        assertEquals("验证成功", result.get("message"));
        assertEquals("machine-001", testCard.getMachineCode());
    }

    @Test
    @DisplayName("更新卡密失败 - 剩余次数不能大于总次数")
    void updateCard_RemainingCountGreaterThanTotalCount_ThrowsException() {
        when(cardRepository.findById(1)).thenReturn(Optional.of(testCard));

        assertThrows(BusinessException.class,
                () -> cardService.updateCard(1, Map.of("totalCount", 10, "remainingCount", 20)));
        verify(cardRepository, never()).save(any(Card.class));
    }

    @Test
    @DisplayName("订单发货失败 - cardSpec 无法解析时不静默回退默认规格")
    void generateCardsForOrder_UnparseableSpec_ThrowsException() {
        // 回归覆盖：历史上解析失败会静默回退 7 天/100 次，发出与用户购买不符的卡
        Order order = new Order();
        order.setCardType("time");
        order.setCardSpec("三个月");
        order.setQuantity(1);

        assertThrows(BusinessException.class, () -> cardService.generateCardsForOrder(order));
        verifyNoInteractions(cardRepository);
    }

    @Test
    @DisplayName("验证成功时记录客户端 IP")
    void verifyCard_Success_RecordsClientIp() {
        when(cardRepository.findByCardKeyForUpdate("TEST-XXXX-YYYY-ZZZZ"))
                .thenReturn(Optional.of(testCard));
        when(cardStatusRepository.findByCardHashForUpdate("encrypted-key-123"))
                .thenReturn(Optional.of(testCardStatus));
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);

        cardService.verifyCard("TEST-XXXX-YYYY-ZZZZ", "machine-001", null, "203.0.113.7");

        assertEquals("203.0.113.7", testCard.getIpAddress());
    }
}
