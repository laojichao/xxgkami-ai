package org.xxg.backend.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .toArray(String[]::new);

        // Safety check: allowCredentials cannot be used with wildcard origins
        for (String origin : origins) {
            if ("*".equals(origin)) {
                throw new IllegalStateException(
                    "CORS 配置错误: allowCredentials=true 时不允许使用通配符 '*' 作为 allowedOrigins。" +
                    "请在 CORS_ALLOWED_ORIGINS 环境变量中配置具体的域名。");
            }
        }

        registry.addMapping("/**")
                .allowedOriginPatterns(origins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
