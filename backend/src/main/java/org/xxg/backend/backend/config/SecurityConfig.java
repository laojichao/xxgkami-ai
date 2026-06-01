package org.xxg.backend.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.xxg.backend.backend.filter.JwtRequestFilter;
import org.xxg.backend.backend.filter.RateLimitFilter;
import org.xxg.backend.backend.filter.RequestMonitorFilter;

/**
 * Spring Security 安全配置。
 * <p>配置无状态会话管理、请求授权规则（公开接口/管理员接口/用户接口）、
 * 以及 JWT 认证、请求监控、限流等过滤器的注册顺序。</p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

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
            .cors(cors -> cors.configure(http))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    // Auth endpoints
                    "/auth/login", "/auth/register", "/auth/register-bind",
                    "/auth/user/login", "/auth/admin/login",
                    "/auth/email-code", "/auth/reset-code", "/auth/reset-password",
                    "/auth/bind/validate", "/auth/refresh",
                    // Public endpoints
                    "/public/**",
                    // Payment callbacks
                    "/payment/notify", "/payment/return",
                    // System
                    "/actuator/**", "/error",
                    "/system/health", "/monitor/check-update",
                    // Card use (open API)
                    "/cards/use", "/cards/verify",
                    // Custom/open API
                    "/custom/**", "/open/**"
                ).permitAll()
                // Admin-only endpoints
                .requestMatchers("/admin/**", "/cards/admin/**", "/cards/generate",
                        "/cards/stats", "/cards/trend", "/users/admin/**",
                        "/settings/**", "/stats/**", "/monitor/**",
                        "/maintenance/**", "/backup/**", "/security/**",
                        "/online-users/**", "/card-pricing/admin/**",
                        "/system/**", "/orders/admin/**").hasRole("ADMIN")
                // User endpoints
                .requestMatchers("/user/**", "/wallet/**", "/orders/**").hasAnyRole("USER", "ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(requestMonitorFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
