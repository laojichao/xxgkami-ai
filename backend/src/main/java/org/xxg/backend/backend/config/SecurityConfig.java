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
