package org.xxg.backend.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.xxg.backend.backend.entity.ApiKey;
import org.xxg.backend.backend.entity.Card;
import org.xxg.backend.backend.mapper.ApiKeyRepository;

import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * Webhook 回调服务。
 * <p>在卡密验证等事件发生时，异步通知第三方配置的回调 URL。
 * 内置 SSRF 防护，禁止向内网/回环地址发送请求。</p>
 */
@Service
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);
    private final ApiKeyRepository apiKeyRepository;
    private final ObjectMapper objectMapper;

    public WebhookService(ApiKeyRepository apiKeyRepository, ObjectMapper objectMapper) {
        this.apiKeyRepository = apiKeyRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 异步触发 Webhook 回调。
     * <p>根据 API Key 中配置的 Webhook URL 和请求方法，发送事件通知。
     * 包含 SSRF 防护，拒绝向内网地址发送请求。</p>
     *
     * @param apiKeyId API Key ID
     * @param card     触发事件的卡密
     * @param event    事件名称（如 "verify"）
     */
    @Async
    @SuppressWarnings("unchecked")
    public void triggerWebhook(Long apiKeyId, Card card, String event) {
        try {
            ApiKey apiKey = apiKeyRepository.findById(apiKeyId.intValue()).orElse(null);
            if (apiKey == null || apiKey.getWebhookConfig() == null || apiKey.getWebhookConfig().isEmpty()) {
                return;
            }

            Map<String, Object> webhookConfig = objectMapper.readValue(apiKey.getWebhookConfig(), Map.class);
            String url = (String) webhookConfig.get("url");
            String method = (String) webhookConfig.getOrDefault("method", "GET");

            if (url == null || url.isEmpty()) return;

            // SSRF protection: validate URL is not internal/loopback
            if (isInternalUrl(url)) {
                log.warn("[WEBHOOK] Blocked internal URL: {}", url);
                return;
            }

            Map<String, Object> payload = new HashMap<>();
            payload.put("event", event);
            payload.put("card_key", card.getCardKey());
            payload.put("card_type", card.getCardType().name());
            payload.put("status", card.getStatus());
            payload.put("timestamp", System.currentTimeMillis());

            String jsonBody = objectMapper.writeValueAsString(payload);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json");

            if ("POST".equalsIgnoreCase(method)) {
                requestBuilder.POST(HttpRequest.BodyPublishers.ofString(jsonBody));
            } else {
                requestBuilder.GET();
            }

            client.sendAsync(requestBuilder.build(), HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        log.debug("[WEBHOOK] Response: {}", response.statusCode());
                    })
                    .exceptionally(e -> {
                        log.warn("[WEBHOOK] Error: {}", e.getMessage());
                        return null;
                    });
        } catch (Exception e) {
            log.warn("[WEBHOOK] Failed: {}", e.getMessage());
        }
    }

    /**
     * 检测 URL 是否指向内网/回环地址（SSRF 防护）。
     * <p>检查项包括：回环地址、站点本地地址、链路本地地址、
     * 常见内网网段（10.x、172.16-31.x、192.168.x、169.254.x）。</p>
     *
     * @param url 待检测的 URL
     * @return true 表示为内网地址，应阻止请求
     */
    private boolean isInternalUrl(String url) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            if (host == null) return true;

            InetAddress addr = InetAddress.getByName(host);
            return addr.isLoopbackAddress() || addr.isSiteLocalAddress()
                    || addr.isLinkLocalAddress() || addr.isAnyLocalAddress()
                    || host.equals("localhost") || host.equals("0.0.0.0")
                    || host.matches("10\\..*") || host.matches("172\\.(1[6-9]|2[0-9]|3[01])\\..*")
                    || host.matches("192\\.168\\..*") || host.matches("169\\.254\\..*");
        } catch (Exception e) {
            return true; // block on parse failure
        }
    }
}
