package org.xxg.backend.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.xxg.backend.backend.entity.Order;
import org.xxg.backend.backend.mapper.OrderRepository;
import org.xxg.backend.backend.util.PaymentUtil;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * PaymentService 单元测试
 * 测试支付回调处理、签名校验、幂等性保护等关键逻辑
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private SettingsService settingsService;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderService orderService;
    @Mock
    private CardService cardService;
    @Mock
    private PaymentUtil paymentUtil;

    @InjectMocks
    private PaymentService paymentService;

    private Order testOrder;

    @BeforeEach
    void setUp() {
        testOrder = new Order();
        testOrder.setId(1);
        testOrder.setOrderNo("ORD20260614001");
        testOrder.setUserId(1);
        testOrder.setUsername("testuser");
        testOrder.setCardType("time");
        testOrder.setCardSpec("7天");
        testOrder.setQuantity(1);
        testOrder.setTotalPrice(new BigDecimal("9.90"));
        testOrder.setStatus("pending");
    }

    @Test
    @DisplayName("支付回调成功 - 签名正确且订单待支付")
    void handlePaymentCallback_Success() {
        Map<String, String> params = new HashMap<>();
        params.put("trade_status", "TRADE_SUCCESS");
        params.put("out_trade_no", "ORD20260614001");
        params.put("money", "9.90");

        when(settingsService.getValue("epay_key", "")).thenReturn("test-key");
        when(paymentUtil.verifySign(params, "test-key")).thenReturn(true);
        when(orderRepository.findByOrderNoWithLock("ORD20260614001"))
                .thenReturn(Optional.of(testOrder));
        when(cardService.generateCardsForOrder(testOrder)).thenReturn("CARD-001,CARD-002");

        String result = paymentService.handlePaymentCallback(params);

        assertEquals("success", result);
        verify(orderService).completeOrder("ORD20260614001", "CARD-001,CARD-002");
    }

    @Test
    @DisplayName("支付回调失败 - 签名校验不通过")
    void handlePaymentCallback_InvalidSignature_ReturnsFail() {
        Map<String, String> params = new HashMap<>();
        params.put("trade_status", "TRADE_SUCCESS");
        params.put("out_trade_no", "ORD20260614001");

        when(settingsService.getValue("epay_key", "")).thenReturn("test-key");
        when(paymentUtil.verifySign(params, "test-key")).thenReturn(false);

        String result = paymentService.handlePaymentCallback(params);

        assertEquals("fail", result);
        verify(orderRepository, never()).findByOrderNoWithLock(any());
    }

    @Test
    @DisplayName("支付回调失败 - 订单号为空")
    void handlePaymentCallback_MissingOrderNo_ReturnsFail() {
        Map<String, String> params = new HashMap<>();
        params.put("trade_status", "TRADE_SUCCESS");
        // 没有 out_trade_no

        when(settingsService.getValue("epay_key", "")).thenReturn("test-key");
        when(paymentUtil.verifySign(params, "test-key")).thenReturn(true);

        String result = paymentService.handlePaymentCallback(params);

        assertEquals("fail", result);
    }

    @Test
    @DisplayName("支付回调幂等性 - 已完成的订单不重复处理")
    void handlePaymentCallback_AlreadyCompleted_Idempotent() {
        testOrder.setStatus("completed"); // 已完成
        Map<String, String> params = new HashMap<>();
        params.put("trade_status", "TRADE_SUCCESS");
        params.put("out_trade_no", "ORD20260614001");
        params.put("money", "9.90");

        when(settingsService.getValue("epay_key", "")).thenReturn("test-key");
        when(paymentUtil.verifySign(params, "test-key")).thenReturn(true);
        when(orderRepository.findByOrderNoWithLock("ORD20260614001"))
                .thenReturn(Optional.of(testOrder));

        String result = paymentService.handlePaymentCallback(params);

        assertEquals("success", result);
        verify(cardService, never()).generateCardsForOrder(any());
    }

    @Test
    @DisplayName("支付回调失败 - 金额不匹配")
    void handlePaymentCallback_AmountMismatch_ReturnsFail() {
        Map<String, String> params = new HashMap<>();
        params.put("trade_status", "TRADE_SUCCESS");
        params.put("out_trade_no", "ORD20260614001");
        params.put("money", "19.90"); // 金额不匹配

        when(settingsService.getValue("epay_key", "")).thenReturn("test-key");
        when(paymentUtil.verifySign(params, "test-key")).thenReturn(true);
        when(orderRepository.findByOrderNoWithLock("ORD20260614001"))
                .thenReturn(Optional.of(testOrder));

        String result = paymentService.handlePaymentCallback(params);

        assertEquals("fail", result);
        verify(cardService, never()).generateCardsForOrder(any());
    }

    @Test
    @DisplayName("支付回调失败 - 订单不存在")
    void handlePaymentCallback_OrderNotFound_ReturnsFail() {
        Map<String, String> params = new HashMap<>();
        params.put("trade_status", "TRADE_SUCCESS");
        params.put("out_trade_no", "NONEXISTENT");
        params.put("money", "9.90");

        when(settingsService.getValue("epay_key", "")).thenReturn("test-key");
        when(paymentUtil.verifySign(params, "test-key")).thenReturn(true);
        when(orderRepository.findByOrderNoWithLock("NONEXISTENT"))
                .thenReturn(Optional.empty());

        String result = paymentService.handlePaymentCallback(params);

        assertEquals("fail", result);
    }

    @Test
    @DisplayName("支付回调 - 非 TRADE_SUCCESS 状态返回 fail")
    void handlePaymentCallback_NonTradeSuccess_ReturnsFail() {
        Map<String, String> params = new HashMap<>();
        params.put("trade_status", "TRADE_CLOSED");
        params.put("out_trade_no", "ORD20260614001");

        when(settingsService.getValue("epay_key", "")).thenReturn("test-key");
        when(paymentUtil.verifySign(params, "test-key")).thenReturn(true);

        String result = paymentService.handlePaymentCallback(params);

        assertEquals("fail", result);
    }

    @Test
    @DisplayName("创建支付订单成功")
    void createPayment_Success() {
        when(orderRepository.findByOrderNo("ORD20260614001"))
                .thenReturn(Optional.of(testOrder));
        when(settingsService.getValue("epay_api_url", ""))
                .thenReturn("https://pay.example.com/");
        when(settingsService.getValue("epay_pid", ""))
                .thenReturn("1001");
        when(settingsService.getValue("epay_key", ""))
                .thenReturn("test-key");
        when(settingsService.getValue("epay_notify_url", ""))
                .thenReturn("https://api.example.com/payment/notify");
        when(settingsService.getValue("epay_return_url", ""))
                .thenReturn("https://example.com/payment/return");
        when(paymentUtil.generateSign(any(), any())).thenReturn("test-sign");

        Map<String, String> result = paymentService.createPayment("ORD20260614001");

        assertNotNull(result.get("url"));
        assertTrue(result.get("url").contains("pay.example.com"));
    }

    @Test
    @DisplayName("创建支付订单失败 - 支付配置不完整")
    void createPayment_IncompleteConfig_ThrowsException() {
        when(orderRepository.findByOrderNo("ORD20260614001"))
                .thenReturn(Optional.of(testOrder));
        when(settingsService.getValue("epay_api_url", "")).thenReturn(""); // 空

        assertThrows(Exception.class,
                () -> paymentService.createPayment("ORD20260614001"));
    }
}
