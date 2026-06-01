package org.xxg.backend.backend.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 请求限流过滤器
 * 基于滑动窗口算法，对登录接口和卡密验证接口进行IP级别的频率限制，
 * 防止暴力破解和恶意刷接口。使用ConcurrentHashMap保证线程安全。
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    /** 登录接口的请求计数器（按IP分组） */
    private final ConcurrentHashMap<String, RequestCounter> loginAttempts = new ConcurrentHashMap<>();
    /** 卡密验证接口的请求计数器（按IP分组） */
    private final ConcurrentHashMap<String, RequestCounter> verifyAttempts = new ConcurrentHashMap<>();

    /** 登录接口：每窗口最大请求数 */
    private static final int LOGIN_MAX_REQUESTS = 10;
    /** 登录接口：滑动窗口时长（秒） */
    private static final int LOGIN_WINDOW_SECONDS = 60;
    /** 验证接口：每窗口最大请求数 */
    private static final int VERIFY_MAX_REQUESTS = 30;
    /** 验证接口：滑动窗口时长（秒） */
    private static final int VERIFY_WINDOW_SECONDS = 60;

    /**
     * 过滤器核心逻辑：根据请求URI和客户端IP进行限流判断
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        String clientIp = getClientIp(request);

        // 登录接口限流：同一IP每分钟最多10次
        if (uri.contains("/auth/admin/login") || uri.contains("/auth/user/login")) {
            if (!tryAcquire(loginAttempts, clientIp, LOGIN_MAX_REQUESTS, LOGIN_WINDOW_SECONDS)) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"success\":false,\"message\":\"登录请求过于频繁，请稍后再试\"}");
                return;
            }
        }

        // 卡密验证接口限流：同一IP每分钟最多30次
        if (uri.contains("/cards/use") || uri.contains("/cards/verify")) {
            if (!tryAcquire(verifyAttempts, clientIp, VERIFY_MAX_REQUESTS, VERIFY_WINDOW_SECONDS)) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"success\":false,\"message\":\"验证请求过于频繁，请稍后再试\"}");
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

        // 使用compute原子操作获取或重置计数器
        RequestCounter counter = counters.compute(key, (k, v) -> {
            if (v == null || now - v.windowStart > windowMs) {
                return new RequestCounter(now);
            }
            return v;
        });

        // 二次检查：防止并发场景下窗口过期未重置
        if (now - counter.windowStart > windowMs) {
            counters.put(key, new RequestCounter(now));
            counter = counters.get(key);
        }

        // 原子递增并判断是否超出限制
        return counter.count.incrementAndGet() <= maxRequests;
    }

    /**
     * 获取客户端真实IP地址
     * 优先从X-Forwarded-For、X-Real-IP头部获取，兜底使用remoteAddr
     * 支持多级代理场景（取第一个IP）
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多个代理时取第一个IP（即客户端真实IP）
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
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
