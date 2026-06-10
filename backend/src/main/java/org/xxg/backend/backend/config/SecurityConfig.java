package org.xxg.backend.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.xxg.backend.backend.filter.JwtRequestFilter;
import org.xxg.backend.backend.filter.RateLimitFilter;
import org.xxg.backend.backend.filter.RequestMonitorFilter;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security 安全配置。
 * <p>配置无状态会话管理、请求授权规则（公开接口/管理员接口/用户接口）、
 * 以及 JWT 认证、请求监控、限流等过滤器的注册顺序。</p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
    private String corsOrigins;

    private final JwtRequestFilter jwtRequestFilter;
    private final RequestMonitorFilter requestMonitorFilter;
    private final RateLimitFilter rateLimitFilter;

    public SecurityConfig(JwtRequestFilter jwtRequestFilter, RequestMonitorFilter requestMonitorFilter,
                          RateLimitFilter rateLimitFilter) {
        this.jwtRequestFilter = jwtRequestFilter;
        this.requestMonitorFilter = requestMonitorFilter;
        this.rateLimitFilter = rateLimitFilter;
    }

    /**
     * CORS 配置源（供 Spring Security 使用）。
     * <p>允许指定源的跨域请求，启用凭据传递以支持 httpOnly Cookie 认证。</p>
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        String[] origins = Arrays.stream(corsOrigins.split(","))
                .map(String::trim)
                .toArray(String[]::new);
        config.setAllowedOrigins(Arrays.asList(origins));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * 配置安全过滤器链。
     * <p>规则说明：</p>
     * <ul>
     *   <li>禁用 CSRF（REST API 无状态认证）</li>
     *   <li>会话策略为 STATELESS（不创建 HttpSession）</li>
     *   <li>公开接口（/auth/login、/public/**、/payment/notify 等）允许匿名访问</li>
     *   <li>管理接口（/admin/**、/cards/admin/** 等）需 ADMIN 角色</li>
     *   <li>用户接口（/user/**、/wallet/** 等）需 USER 或 ADMIN 角色</li>
     *   <li>过滤器顺序：请求监控 -> 限流 -> JWT 认证</li>
     * </ul>
     *
     * @param http HttpSecurity 配置对象
     * @return 构建好的 SecurityFilterChain
     * @throws Exception 配置异常
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // === 公开接口 ===
                .requestMatchers(
                    "/auth/login", "/auth/register", "/auth/register-bind",
                    "/auth/user/login", "/auth/admin/login",
                    "/auth/email-code", "/auth/reset-code", "/auth/reset-password",
                    "/auth/bind/validate", "/auth/refresh",
                    "/auth/oauth/set-cookies",
                    "/public/**",
                    "/payment/notify", "/payment/return",
                    "/system/health", "/error",
                    "/cards/use", "/cards/verify",
                    "/custom/**", "/open/**"
                ).permitAll()
                // === Pricing：GET 公开，写操作仅管理员 ===
                .requestMatchers(HttpMethod.GET, "/pricing", "/pricing/**").permitAll()
                .requestMatchers("/pricing", "/pricing/**").hasRole("ADMIN")
                // === 卡密管理：DELETE/PUT 写操作仅管理员 ===
                .requestMatchers(HttpMethod.DELETE, "/cards/*").hasRole("ADMIN")
                .requestMatchers("/cards/*/disable", "/cards/*/enable", "/cards/*/unbind").hasRole("ADMIN")
                // === 管理员专属接口 ===
                .requestMatchers("/admin/**", "/cards/admin/**", "/cards/generate",
                        "/cards/stats", "/cards/trend", "/users/admin/**",
                        "/settings/**", "/stats/**", "/monitor/**",
                        "/maintenance/**", "/backup/**", "/security/**",
                        "/online-users/**", "/card-pricing/admin/**",
                        "/system/**", "/orders/admin/**",
                        "/user/admin/**", "/user/stats",
                        "/actuator/**").hasRole("ADMIN")
                // === 在线用户接口：需认证（防止未登录用户伪造上线状态） ===
                .requestMatchers("/online/login", "/online/logout", "/online/heartbeat").authenticated()
                // === 用户接口 ===
                .requestMatchers("/user/profile", "/user/password", "/user/avatar",
                        "/user/social/**", "/wallet/**", "/orders").hasAnyRole("USER", "ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(requestMonitorFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
