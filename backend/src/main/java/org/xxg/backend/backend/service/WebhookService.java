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
     * <p>安全校验：apiKeyId 必须与卡密的 creatorId 匹配，防止攻击者利用
     * 任意 apiKeyId 触发其他用户的 Webhook。</p>
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

            // Security: verify the API key is the one associated with this card,
            // preventing attackers from triggering webhooks for other users' API keys
            if (card.getApiKeyId() != null && !card.getApiKeyId().equals(apiKeyId)) {
                log.warn("[WEBHOOK] Blocked cross-user webhook: card.apiKeyId={} but request apiKeyId={}",
                        card.getApiKeyId(), apiKeyId);
                return;
            }

            Map<String, Object> webhookConfig = objectMapper.readValue(apiKey.getWebhookConfig(), Map.class);
            String url = (String) webhookConfig.get("url");
            String method = (String) webhookConfig.getOrDefault("method", "GET");

            if (url == null || url.isEmpty()) return;

            // SSRF protection: validate URL is not internal/loopback
            // 安全修复：解析 IP 后使用解析到的 IP 直接发起请求，设置 Host 头，防止 DNS rebinding
            URI originalUri = URI.create(url);
            String resolvedIp = resolveAndValidateHost(originalUri);
            if (resolvedIp == null) {
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

            // 构建使用解析后 IP 的 URI，并保留原始端口和路径
            int port = originalUri.getPort();
            String scheme = originalUri.getScheme();
            String path = originalUri.getRawPath() + (originalUri.getRawQuery() != null ? "?" + originalUri.getRawQuery() : "");
            String ipUriStr = scheme + "://" + resolvedIp + (port != -1 ? ":" + port : "") + path;

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(ipUriStr))
                    .header("Content-Type", "application/json")
                    // 设置 Host 头为原始主机名，保证服务端虚拟主机路由正常
                    .header("Host", originalUri.getHost());

            if ("POST".equalsIgnoreCase(method)) {
                requestBuilder.POST(HttpRequest.BodyPublishers.ofString(jsonBody));
            } else {
                requestBuilder.GET();
            }

            httpClient.sendAsync(requestBuilder.build(), HttpResponse.BodyHandlers.ofString())
                    .orTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
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
     * 解析 URL 主机并校验是否为内网/回环地址，返回解析后的 IP 字符串。
     * <p>安全修复：解析 IP 后返回 IP 字符串供调用方直接使用，避免二次 DNS 解析导致 DNS rebinding 攻击。
     * 调用方应使用返回的 IP 构建请求 URI，并设置 Host 头为原始主机名。</p>
     *
     * @param uri 待校验的 URI
     * @return 解析后的 IP 字符串（内网地址返回 null 表示阻止）
     */
    private String resolveAndValidateHost(URI uri) {
        try {
            // 仅允许 http/https 协议，阻止 file:/gopher:/ftp: 等协议的 SSRF
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                return null;
            }
            String host = uri.getHost();
            if (host == null) return null;

            // 阻止明确的内部主机名
            String lowerHost = host.toLowerCase();
            if (lowerHost.equals("localhost") || lowerHost.equals("0.0.0.0")) {
                return null;
            }

            // Check cloud metadata hostnames
            if (lowerHost.equals("metadata.google.internal") || lowerHost.equals("metadata.google.com") ||
                lowerHost.equals("instance-data") || lowerHost.equals("169.254.169.254")) {
                return null;
            }

            // 解析 IP 地址（包括 IPv6），仅解析一次，避免 DNS rebinding
            InetAddress addr = InetAddress.getByName(host);
            String resolvedIp = addr.getHostAddress();

            // Check CGNAT range (100.64.0.0/10)
            if (addr.getAddress().length == 4) {
                int firstOctet = addr.getAddress()[0] & 0xFF;
                int secondOctet = addr.getAddress()[1] & 0xFF;
                if (firstOctet == 100 && (secondOctet >= 64 && secondOctet <= 127)) {
                    return null;
                }
            }

            if (addr.isLoopbackAddress() || addr.isSiteLocalAddress()
                    || addr.isLinkLocalAddress() || addr.isAnyLocalAddress()) {
                return null;
            }
            return resolvedIp;
        } catch (Exception e) {
            return null; // 解析失败时阻止请求
        }
    }
}
