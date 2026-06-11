package org.xxg.backend.backend.filter;

import jakarta.annotation.PreDestroy;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 请求限流过滤器
 * 基于滑动窗口算法，对登录接口和卡密验证接口进行IP级别的频率限制，
 * 防止暴力破解和恶意刷接口。使用ConcurrentHashMap保证线程安全。
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    /**
     * 可信代理 IP 列表（来自 application.properties 配置）。
     * <p>仅当请求来自这些 IP 时，才信任 X-Forwarded-For 头部。
     * 默认包含常见的反向代理地址。</p>
     */
    @Value("${rate-limit.trusted-proxies:127.0.0.1,0:0:0:0:0:0:0:1}")
    private Set<String> trustedProxies;

    /** 登录接口的请求计数器（按IP分组） */
    private final ConcurrentHashMap<String, RequestCounter> loginAttempts = new ConcurrentHashMap<>();
    /** 卡密验证接口的请求计数器（按IP分组） */
    private final ConcurrentHashMap<String, RequestCounter> verifyAttempts = new ConcurrentHashMap<>();
    /** 敏感认证接口（注册/邮箱验证码/密码重置）的请求计数器（按IP分组） */
    private final ConcurrentHashMap<String, RequestCounter> sensitiveAttempts = new ConcurrentHashMap<>();
    /** 公开接口（支付回调/卡密查询解绑）的请求计数器（按IP分组） */
    private final ConcurrentHashMap<String, RequestCounter> publicAttempts = new ConcurrentHashMap<>();

    /** 定时清理过期计数器，防止内存泄漏（每5分钟执行一次） */
    private static final long CLEANUP_INTERVAL_MINUTES = 5;
    /** 计数器过期时间（10分钟无活动即清理） */
    private static final long ENTRY_EXPIRE_MS = 10 * 60 * 1000L;

    /** 登录接口：每窗口最大请求数 */
    private static final int LOGIN_MAX_REQUESTS = 10;
    /** 登录接口：滑动窗口时长（秒） */
    private static final int LOGIN_WINDOW_SECONDS = 60;
    /** 验证接口：每窗口最大请求数 */
    private static final int VERIFY_MAX_REQUESTS = 30;
    /** 验证接口：滑动窗口时长（秒） */
    private static final int VERIFY_WINDOW_SECONDS = 60;
    /** 敏感认证接口：每窗口最大请求数 */
    private static final int SENSITIVE_MAX_REQUESTS = 5;
    /** 敏感认证接口：滑动窗口时长（秒） */
    private static final int SENSITIVE_WINDOW_SECONDS = 60;
    /** 公开接口：每窗口最大请求数 */
    private static final int PUBLIC_MAX_REQUESTS = 20;
    /** 公开接口：滑动窗口时长（秒） */
    private static final int PUBLIC_WINDOW_SECONDS = 60;

    /** 定时清理调度器 */
    private final ScheduledExecutorService cleanupScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "rate-limit-cleanup");
        t.setDaemon(true);
        return t;
    });

    {
        // 启动定时清理任务，每5分钟清理一次过期的限流计数器
        cleanupScheduler.scheduleAtFixedRate(this::cleanupExpiredEntries,
                CLEANUP_INTERVAL_MINUTES, CLEANUP_INTERVAL_MINUTES, TimeUnit.MINUTES);
    }

    /** 应用关闭时清理定时器线程，防止资源泄漏 */
    @PreDestroy
    public void destroy() {
        cleanupScheduler.shutdownNow();
    }

    /**
     * 清理过期的限流计数器条目，防止内存无限增长。
     * 遍历两个计数器 Map，移除超过 ENTRY_EXPIRE_MS 未更新的条目。
     */
    private void cleanupExpiredEntries() {
        long now = System.currentTimeMillis();
        cleanupMap(loginAttempts, now);
        cleanupMap(verifyAttempts, now);
        cleanupMap(sensitiveAttempts, now);
        cleanupMap(publicAttempts, now);
    }

    private void cleanupMap(ConcurrentHashMap<String, RequestCounter> map, long now) {
        Iterator<Map.Entry<String, RequestCounter>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, RequestCounter> entry = it.next();
            if (now - entry.getValue().windowStart > ENTRY_EXPIRE_MS) {
                it.remove();
            }
        }
    }

    /**
     * 过滤器核心逻辑：根据请求URI和客户端IP进行限流判断
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        String clientIp = getClientIp(request);

        // 登录接口限流：同一IP每分钟最多10次
        if (uri.endsWith("/auth/admin/login") || uri.endsWith("/auth/user/login")) {
            if (!tryAcquire(loginAttempts, clientIp, LOGIN_MAX_REQUESTS, LOGIN_WINDOW_SECONDS)) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"success\":false,\"message\":\"登录请求过于频繁，请稍后再试\"}");
                return;
            }
        }

        // 卡密验证接口限流：同一IP每分钟最多30次
        if (uri.endsWith("/cards/use") || uri.endsWith("/cards/verify")) {
            if (!tryAcquire(verifyAttempts, clientIp, VERIFY_MAX_REQUESTS, VERIFY_WINDOW_SECONDS)) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"success\":false,\"message\":\"验证请求过于频繁，请稍后再试\"}");
                return;
            }
        }

        // 敏感认证接口限流：同一IP每分钟最多5次（防止邮箱轰炸和验证码暴力破解）
        if (uri.endsWith("/auth/register") || uri.endsWith("/auth/email-code")
                || uri.endsWith("/auth/reset-code") || uri.endsWith("/auth/reset-password")
                || uri.endsWith("/auth/totp/enable") || uri.endsWith("/auth/totp/disable")
                || uri.endsWith("/auth/totp/setup")) {
            if (!tryAcquire(sensitiveAttempts, clientIp, SENSITIVE_MAX_REQUESTS, SENSITIVE_WINDOW_SECONDS)) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"success\":false,\"message\":\"请求过于频繁，请稍后再试\"}");
                return;
            }
        }

        // 公开接口限流：支付回调和卡密查询/解绑，防止卡密枚举和支付回调滥用
        if (uri.endsWith("/payment/notify")
                || uri.endsWith("/public/cards/machine-bind/query")
                || uri.endsWith("/public/cards/machine-bind/unbind")) {
            if (!tryAcquire(publicAttempts, clientIp, PUBLIC_MAX_REQUESTS, PUBLIC_WINDOW_SECONDS)) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"success\":false,\"message\":\"请求过于频繁，请稍后再试\"}");
                return;
            }
        }

        // 未触发限流，放行请求
        filterChain.doFilter(request, response);
    }

    /**
     * 尝试获取请求许可（滑动窗口限流算法）
     * @param counters 计数器映射表
     * @param key 限流键（通常是IP地址）
     * @param maxRequests 窗口内最大允许请求数
     * @param windowSeconds 窗口时长（秒）
     * @return 未超限返回true，已超限返回false
     */
    private boolean tryAcquire(ConcurrentHashMap<String, RequestCounter> counters,
                               String key, int maxRequests, int windowSeconds) {
        long now = System.currentTimeMillis();
        long windowMs = windowSeconds * 1000L;

        // 使用 compute 原子操作获取或重置计数器，消除竞态条件
        RequestCounter counter = counters.compute(key, (k, v) -> {
            if (v == null || now - v.windowStart > windowMs) {
                return new RequestCounter(now);
            }
            return v;
        });

        // 原子递增并判断是否超出限制
        return counter.count.incrementAndGet() <= maxRequests;
    }

    /**
     * 获取客户端IP地址。
     * <p>安全策略：当请求来自可信代理时，从 X-Forwarded-For 头部提取真实客户端 IP；
     * 否则使用 remoteAddr（TCP 连接的直接来源），防止攻击者通过伪造头部绕过限流。</p>
     */
    private String getClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        // 仅当请求来自可信代理时，才信任 X-Forwarded-For
        if (trustedProxies.contains(remoteAddr)) {
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isEmpty()) {
                // X-Forwarded-For 格式：client, proxy1, proxy2；取第一个即真实客户端 IP
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

    /**
     * 请求计数器（内部类）
     * 记录滑动窗口的起始时间和请求次数
     */
    private static class RequestCounter {
        /** 窗口起始时间戳（毫秒） */
        final long windowStart;
        /** 窗口内的请求计数（原子操作保证线程安全） */
        final AtomicInteger count;

        RequestCounter(long windowStart) {
            this.windowStart = windowStart;
            this.count = new AtomicInteger(0);
        }
    }
}
