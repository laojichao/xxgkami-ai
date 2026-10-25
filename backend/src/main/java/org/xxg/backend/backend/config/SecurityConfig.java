package org.xxg.backend.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
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
@EnableMethodSecurity
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
        // Safety check: allowCredentials cannot be used with wildcard origins
        for (String origin : origins) {
            if ("*".equals(origin)) {
                throw new IllegalStateException(
                    "CORS 配置错误: allowCredentials=true 时不允许使用通配符 '*' 作为 allowedOrigins。" +
                    "请在 CORS_ALLOWED_ORIGINS 环境变量中配置具体的域名。");
            }
        }
        config.setAllowedOrigins(Arrays.asList(origins));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With", "Origin"));
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
            .headers(headers -> headers
                .frameOptions(frame -> frame.deny())
                // 安全响应头：防止 MIME 嗅探、强制 HTTPS、控制 Referrer 泄露
                .contentTypeOptions(contentType -> {}) // X-Content-Type-Options: nosniff
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000)) // Strict-Transport-Security
                .referrerPolicy(referrer -> referrer
                    .policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .cacheControl(cache -> {}) // 禁止缓存敏感响应
            )
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 历史缺陷：未配置 exceptionHandling 时，Spring Security 6 对未认证请求
            // 返回 403 而非 401（且响应体为容器默认错误页）。前端/移动端均以 401
            // 作为"token 过期需静默刷新"的信号（api.js response.status === 401 分支、
            // ApiClient 的 Unauthorized 重试），403 语义会使 access_token 到期（1h）后
            // 刷新流程永不触发，用户直接掉线且误报"权限不足"。
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) -> {
                    res.setStatus(401);
                    res.setContentType("application/json;charset=UTF-8");
                    res.getWriter().write("{\"success\":false,\"message\":\"未登录或登录已过期\"}");
                })
                .accessDeniedHandler((req, res, ex) -> {
                    res.setStatus(403);
                    res.setContentType("application/json;charset=UTF-8");
                    res.getWriter().write("{\"success\":false,\"message\":\"权限不足\"}");
                }))
            .authorizeHttpRequests(auth -> auth
                // === 公开接口 ===
                .requestMatchers(
                    "/auth/register", "/auth/register-bind",
                    "/auth/user/login", "/auth/admin/login",
                    "/auth/email-code", "/auth/reset-code", "/auth/reset-password",
                    "/auth/bind/validate", "/auth/refresh",
                    "/auth/oauth/state", "/auth/oauth/set-cookies",
                    "/auth/totp/disable-by-recovery",
                    // 登出必须放行：token 已过期时该接口落在 anyRequest().authenticated()
                    // 会返回 401，客户端无法清除本地 httpOnly Cookie，陷入
                    // "登不出去"死局。控制器内部已对 auth==null 做了完整处理（跳过
                    // 服务端 token 清理，仅清除 Cookie），公开访问无安全影响
                    "/auth/logout",
                    "/public/**",
                    "/payment/notify", "/payment/return",
                    "/system/health", "/error",
                    "/cards/use", "/cards/verify"
                ).permitAll()
                // 维护状态仅返回开关与公告文案，对访客公开（前端维护遮罩依赖此接口）
                .requestMatchers(HttpMethod.GET, "/maintenance/status").permitAll()
                // === Pricing：GET 公开，写操作仅管理员 ===
                // card-pricing 与 pricing 是同一控制器的别名路径，权限规则必须一致；
                // 历史缺陷：仅 /pricing 写操作受限，别名 /card-pricing 的写操作落入
                // anyRequest().authenticated()，普通用户可改价 0.01 元购卡。
                .requestMatchers(HttpMethod.GET, "/pricing", "/pricing/**",
                        "/card-pricing", "/card-pricing/**").permitAll()
                .requestMatchers("/pricing", "/pricing/**",
                        "/card-pricing", "/card-pricing/**").hasRole("ADMIN")
                // === 卡密管理：用户查询自己的卡密 ===
                .requestMatchers("/cards/user/me").hasAnyRole("USER", "ADMIN")
                // === 卡密管理：查询/删除/启停用/解绑 仅管理员 ===
                .requestMatchers("/cards/query", "/cards/user/**", "/cards/apikey/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/cards/*").hasRole("ADMIN")
                .requestMatchers("/cards/*/disable", "/cards/*/enable", "/cards/*/unbind").hasRole("ADMIN")
                // === 管理员专属接口 ===
                .requestMatchers("/admin/**", "/cards/admin/**", "/cards/generate",
                        "/cards/stats", "/cards/trend", "/users/admin/**",
                        "/settings/**", "/stats/**", "/monitor/**",
                        "/maintenance/**", "/backup/**", "/security/**",
                        "/online-users/**", "/online/list", "/online/check/**", "/card-pricing/admin/**",
                        "/system/**", "/orders/admin/**", "/orders/stats",
                        "/user/admin/**", "/user/stats",
                        "/actuator/**", "/webhook-test",
                        // 钱包充值仅管理员可操作（防止用户直接充值绕过支付）
                        "/wallet/recharge",
                        // Swagger UI / OpenAPI 仅管理员可访问（生产环境不应公开）
                        "/swagger-ui/**", "/swagger-ui.html",
                        "/v3/api-docs/**", "/v3/api-docs").hasRole("ADMIN")
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
