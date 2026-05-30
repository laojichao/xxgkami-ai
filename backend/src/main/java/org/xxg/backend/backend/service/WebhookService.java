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

@Service
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);
    private final ApiKeyRepository apiKeyRepository;
    private final ObjectMapper objectMapper;

    public WebhookService(ApiKeyRepository apiKeyRepository, ObjectMapper objectMapper) {
        this.apiKeyRepository = apiKeyRepository;
        this.objectMapper = objectMapper;
    }

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
