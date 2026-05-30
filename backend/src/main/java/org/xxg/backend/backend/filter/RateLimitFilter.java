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

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final ConcurrentHashMap<String, RequestCounter> loginAttempts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, RequestCounter> verifyAttempts = new ConcurrentHashMap<>();

    private static final int LOGIN_MAX_REQUESTS = 10;
    private static final int LOGIN_WINDOW_SECONDS = 60;
    private static final int VERIFY_MAX_REQUESTS = 30;
    private static final int VERIFY_WINDOW_SECONDS = 60;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        String clientIp = getClientIp(request);

        if (uri.contains("/auth/admin/login") || uri.contains("/auth/user/login")) {
            if (!tryAcquire(loginAttempts, clientIp, LOGIN_MAX_REQUESTS, LOGIN_WINDOW_SECONDS)) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"success\":false,\"message\":\"登录请求过于频繁，请稍后再试\"}");
                return;
            }
        }

        if (uri.contains("/cards/use") || uri.contains("/cards/verify")) {
            if (!tryAcquire(verifyAttempts, clientIp, VERIFY_MAX_REQUESTS, VERIFY_WINDOW_SECONDS)) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"success\":false,\"message\":\"验证请求过于频繁，请稍后再试\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean tryAcquire(ConcurrentHashMap<String, RequestCounter> counters,
                               String key, int maxRequests, int windowSeconds) {
        long now = System.currentTimeMillis();
        long windowMs = windowSeconds * 1000L;

        RequestCounter counter = counters.compute(key, (k, v) -> {
            if (v == null || now - v.windowStart > windowMs) {
                return new RequestCounter(now);
            }
            return v;
        });

        if (now - counter.windowStart > windowMs) {
            counters.put(key, new RequestCounter(now));
            counter = counters.get(key);
        }

        return counter.count.incrementAndGet() <= maxRequests;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private static class RequestCounter {
        final long windowStart;
        final AtomicInteger count;

        RequestCounter(long windowStart) {
            this.windowStart = windowStart;
            this.count = new AtomicInteger(0);
        }
    }
}
