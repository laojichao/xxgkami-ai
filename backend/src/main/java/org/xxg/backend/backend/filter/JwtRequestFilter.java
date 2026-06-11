package org.xxg.backend.backend.filter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.xxg.backend.backend.entity.User;
import org.xxg.backend.backend.mapper.AdminRepository;
import org.xxg.backend.backend.mapper.UserRepository;
import org.xxg.backend.backend.util.JwtUtil;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * JWT 认证过滤器。
 * <p>从请求的 httpOnly Cookie 或 Authorization 头部提取 Token，
 * 验证有效性后将用户身份信息写入 Spring Security 上下文。</p>
 * <p>使用 Caffeine 缓存（TTL 30秒）减少数据库查询频率，
 * 同时保证账号禁用/降权等变更在 30 秒内生效。</p>
 */
@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtRequestFilter.class);
    private static final List<String> VALID_ROLES = Arrays.asList("admin", "user");
    private final JwtUtil jwtUtil;
    private final AdminRepository adminRepository;
    private final UserRepository userRepository;

    /**
     * 账号有效性缓存：key = "username:role", value = 是否有效。
     * <p>TTL 30秒：管理员禁用用户后最多 30 秒生效，
     * 避免每个请求都查数据库。</p>
     */
    private final Cache<String, Boolean> accountValidityCache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(30, TimeUnit.SECONDS)
            .build();

    public JwtRequestFilter(JwtUtil jwtUtil, AdminRepository adminRepository, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.adminRepository = adminRepository;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 优先从 httpOnly Cookie 中获取 Token
        String token = null;
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("access_token".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }

        // Cookie 中未找到，回退到 Authorization 头部（兼容旧客户端或移动端）
        if (token == null) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }
        }

        if (token != null) {
            try {
                // 验证 Token 有效性且必须是 Access Token（非 Refresh Token）
                if (jwtUtil.isTokenValid(token) && jwtUtil.isAccessToken(token)) {
                    String username = jwtUtil.extractUsername(token);
                    String role = jwtUtil.extractRole(token);

                    // 校验角色是否合法，防止 JWT 被伪造时注入任意权限
                    if (role == null || !VALID_ROLES.contains(role.toLowerCase())) {
                        log.warn("JWT 中包含无效角色: {}", role);
                    } else if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        // 验证用户/管理员在数据库中仍然有效且未被禁用（带缓存）
                        if (!isAccountValid(username, role)) {
                            log.debug("账号 {} 角色 {} 已失效或被禁用，拒绝认证", username, role);
                        } else {
                            SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role.toUpperCase());
                            UsernamePasswordAuthenticationToken authToken =
                                    new UsernamePasswordAuthenticationToken(username, null,
                                            Collections.singletonList(authority));
                            SecurityContextHolder.getContext().setAuthentication(authToken);
                        }
                    }
                }
            } catch (Exception e) {
                // Token 无效，跳过认证，继续执行后续过滤器
                log.debug("JWT validation failed: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 验证账号在数据库中是否仍然有效（带 Caffeine 缓存）。
     * <p>防止以下场景：</p>
     * <ul>
     *   <li>管理员被禁用后，旧 JWT 仍能访问管理接口</li>
     *   <li>用户被降权后，旧 JWT 仍持有高权限角色</li>
     *   <li>账号被删除后，旧 JWT 仍能通过认证</li>
     * </ul>
     * <p>缓存 TTL 30秒，最多存储 10,000 个条目。</p>
     */
    private boolean isAccountValid(String username, String role) {
        String cacheKey = username + ":" + role;
        Boolean cached = accountValidityCache.getIfPresent(cacheKey);
        if (cached != null) return cached;

        boolean valid = checkAccountInDb(username, role);
        accountValidityCache.put(cacheKey, valid);
        return valid;
    }

    /**
     * 查询数据库验证账号有效性。
     */
    private boolean checkAccountInDb(String username, String role) {
        if ("admin".equals(role)) {
            return adminRepository.findByUsername(username)
                    .map(admin -> admin.getFailedLoginAttempts() == null
                            || admin.getFailedLoginAttempts() < 5
                            || admin.getLockTime() == null
                            || admin.getLockTime().plusMinutes(15).isBefore(LocalDateTime.now()))
                    .orElse(false);
        } else if ("user".equals(role)) {
            return userRepository.findByUsername(username)
                    .map(User::getStatus)
                    .orElse(false);
        }
        return false;
    }

    /**
     * 使指定账号的缓存失效（供其他服务在禁用/删除用户时调用）。
     */
    public void invalidateAccountCache(String username, String role) {
        accountValidityCache.invalidate(username + ":" + role);
    }
}
