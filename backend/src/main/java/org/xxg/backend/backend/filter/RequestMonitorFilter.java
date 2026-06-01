package org.xxg.backend.backend.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 请求监控过滤器。
 * <p>记录每个 API 请求的耗时信息，对超过 3 秒的慢请求输出告警日志，
 * 便于排查性能瓶颈。</p>
 */
@Component
public class RequestMonitorFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestMonitorFilter.class);

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
        }
    }

    /**
     * 获取客户端真实 IP 地址。
     * <p>优先从 X-Forwarded-For 头部获取，其次 X-Real-IP，最后使用远程地址。
     * 若存在多个代理 IP，取第一个（即客户端真实 IP）。</p>
     *
     * @param request HTTP 请求
     * @return 客户端 IP 地址
     */
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
}
