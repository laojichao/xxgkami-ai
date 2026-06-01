package org.xxg.backend.backend.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.xxg.backend.backend.util.JwtUtil;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT 认证过滤器。
 * <p>从请求的 Authorization 头部提取 Bearer Token，验证其有效性后将用户身份信息
 * 写入 Spring Security 上下文，实现无状态认证。</p>
 */
@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtRequestFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    /**
     * 拦截每个请求，提取并验证 JWT Token。
     *
     * @param request     HTTP 请求
     * @param response    HTTP 响应
     * @param filterChain 过滤器链
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 从 Authorization 头部获取 Token
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            // 截取 "Bearer " 前缀后的 Token 字符串
            String token = authHeader.substring(7);
            try {
                // 验证 Token 有效性且必须是 Access Token（非 Refresh Token）
                if (jwtUtil.isTokenValid(token) && jwtUtil.isAccessToken(token)) {
                    String username = jwtUtil.extractUsername(token);
                    String role = jwtUtil.extractRole(token);

                    // 当前上下文无认证信息时，构建认证对象并写入 SecurityContext
                    if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role.toUpperCase());
                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(username, null,
                                        Collections.singletonList(authority));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                }
            } catch (Exception e) {
                // Token 无效，跳过认证，继续执行后续过滤器
            }
        }

        filterChain.doFilter(request, response);
    }
}
