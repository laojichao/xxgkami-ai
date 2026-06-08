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
    // 复用 HttpClient 实例，避免每次请求创建新的连接池
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NEVER) // 禁止跟随重定向，防止 SSRF 重定向绕过
            .build();

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
    public void triggerWebhook(Integer apiKeyId, Card card, String event) {
        try {
            ApiKey apiKey = apiKeyRepository.findById(apiKeyId).orElse(null);
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
            // 不发送明文卡密，仅发送脱敏后的标识（前4位 + *** + 后4位）
            String cardKey = card.getCardKey();
            if (cardKey != null && cardKey.length() > 8) {
                payload.put("card_key", cardKey.substring(0, 4) + "***" + cardKey.substring(cardKey.length() - 4));
            } else {
                payload.put("card_key", "***");
            }
            payload.put("card_type", card.getCardType().name());
            payload.put("status", card.getStatus());
            payload.put("timestamp", System.currentTimeMillis());

            String jsonBody = objectMapper.writeValueAsString(payload);

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json");

            if ("POST".equalsIgnoreCase(method)) {
                requestBuilder.POST(HttpRequest.BodyPublishers.ofString(jsonBody));
            } else {
                requestBuilder.GET();
            }

            httpClient.sendAsync(requestBuilder.build(), HttpResponse.BodyHandlers.ofString())
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

            // 阻止明确的内部主机名
            String lowerHost = host.toLowerCase();
            if (lowerHost.equals("localhost") || lowerHost.equals("0.0.0.0")) {
                return true;
            }

            // 解析 IP 地址（包括 IPv6）
            InetAddress addr = InetAddress.getByName(host);
            // 使用已解析的 IP 重新检查，防止 DNS rebinding
            String resolvedIp = addr.getHostAddress();
            InetAddress resolvedAddr = InetAddress.getByName(resolvedIp);

            return resolvedAddr.isLoopbackAddress() || resolvedAddr.isSiteLocalAddress()
                    || resolvedAddr.isLinkLocalAddress() || resolvedAddr.isAnyLocalAddress();
        } catch (Exception e) {
            return true; // 解析失败时阻止请求
        }
    }
}
