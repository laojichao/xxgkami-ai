package org.xxg.backend.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

/**
 * Web MVC 配置。
 * <p>CORS 策略由 SecurityConfig 中的 CorsConfigurationSource bean 统一管理，
 * 此处不再重复配置，避免两处 CORS 规则不一致导致难以排查的问题。</p>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // CORS is handled by SecurityConfig.corsConfigurationSource()
        // Intentionally left empty to avoid duplicate/conflicting CORS rules.
        // The wildcard safety check is still performed at startup via SecurityConfig.
    }
}
