package org.xxg.backend.backend.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.xxg.backend.backend.service.SecurityService;

import java.io.IOException;
import java.util.Set;

/**
 * 请求监控过滤器。
 * <p>记录每个 API 请求的耗时信息，对超过 3 秒的慢请求输出告警日志，
 * 便于排查性能瓶颈。同时拦截 IP 黑名单中的请求。</p>
 */
@Component
public class RequestMonitorFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestMonitorFilter.class);

    /** User-Agent 最大记录长度（实体字段为 TEXT，此处限制避免异常超长头部写入） */
    private static final int MAX_USER_AGENT_LENGTH = 500;

    private final SecurityService securityService;

    @Value("${rate-limit.trusted-proxies:127.0.0.1,0:0:0:0:0:0:0:1}")
    private Set<String> trustedProxies;

    public RequestMonitorFilter(SecurityService securityService) {
        this.securityService = securityService;
    }

    /**
     * 拦截请求并计算处理耗时，慢请求写入告警日志。
     *
     * @param request     HTTP 请求
     * @param response    HTTP 响应
     * @param filterChain 过滤器链
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long startTime = System.currentTimeMillis();
        String clientIp = getClientIp(request);
        String uri = request.getRequestURI();
        String method = request.getMethod();

        // Check IP blacklist before processing the request
        if (securityService.isIpBlocked(clientIp)) {
            log.warn("[BLOCKED IP] {} | {} {} | IP: {} is blacklisted",
                    java.time.LocalDateTime.now(), method, uri, clientIp);
            response.setStatus(403);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":403,\"message\":\"您的IP已被封禁\"}");
            return;
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            // 计算请求处理耗时
            long duration = System.currentTimeMillis() - startTime;
            int status = response.getStatus();

            // 慢请求告警阈值：3 秒
            if (duration > 3000) {
                log.warn("[SLOW REQUEST] {} | {} {} | IP: {} | Duration: {}ms | Status: {}",
                        java.time.LocalDateTime.now(), method, uri, clientIp, duration, status);
            }

            // 持久化访问日志，供管理后台「访问日志」页面查询。
            // 此前 SecurityService.logAccess 从未被调用，导致该功能始终返回空列表。
            // 使用 @Async 异步写入：日志落库不应给每个请求增加数据库往返延迟，
            // 失败也只记录 debug 日志，不影响业务响应。
            if (shouldRecordAccessLog(uri)) {
                try {
                    securityService.logAccessAsync(clientIp, method, uri,
                            truncate(request.getHeader("User-Agent"), MAX_USER_AGENT_LENGTH),
                            status, duration, resolveUsername());
                } catch (Exception e) {
                    log.debug("访问日志提交失败: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * 判断是否需要记录访问日志。
     * <p>排除健康检查、静态资源等高频无审计价值的请求，避免日志表膨胀。</p>
     *
     * @param uri 请求路径
     * @return true 表示需要记录
     */
    private boolean shouldRecordAccessLog(String uri) {
        if (uri == null) return false;
        return !uri.contains("/system/health")
                && !uri.contains("/actuator/")
                && !uri.contains("/uploads/")
                && !uri.contains("/favicon");
    }

    /**
     * 获取当前请求已认证的用户名（未认证返回 null）。
     * <p>本方法在 {@code filterChain.doFilter()} 返回后的 finally 块中调用，
     * 此时 JwtRequestFilter（注册顺序在本过滤器之后、更靠近
     * UsernamePasswordAuthenticationFilter）已完成认证并填充 SecurityContext，
     * 因此可以读取到用户名；匿名请求返回 null。</p>
     */
    private String resolveUsername() {
        try {
            var authentication = org.springframework.security.core.context.SecurityContextHolder
                    .getContext().getAuthentication();
            return authentication != null ? authentication.getName() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /** 截断过长字符串，防止超出数据库列长度 */
    private String truncate(String value, int maxLength) {
        if (value == null) return null;
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    /**
     * 获取客户端真实 IP 地址。
     * <p>当请求来自可信代理时，从 X-Forwarded-For 头部提取真实客户端 IP；
     * 否则使用 remoteAddr，防止攻击者通过伪造头部绕过 IP 黑名单。</p>
     *
     * @param request HTTP 请求
     * @return 客户端 IP 地址
     */
    private String getClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (trustedProxies.contains(remoteAddr)) {
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isEmpty()) {
                String clientIp = xff.split(",")[0].trim();
                if (!clientIp.isEmpty()) {
                    return clientIp;
                }
            }
            String xRealIp = request.getHeader("X-Real-IP");
            if (xRealIp != null && !xRealIp.isEmpty()) {
                return xRealIp.trim();
            }
        }
        return remoteAddr;
    }
}
