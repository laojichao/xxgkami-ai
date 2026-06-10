package org.xxg.backend.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xxg.backend.backend.entity.AccessLog;
import org.xxg.backend.backend.entity.IpBlacklist;
import org.xxg.backend.backend.mapper.AccessLogRepository;
import org.xxg.backend.backend.mapper.IpBlacklistRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 安全服务。
 * <p>提供 IP 黑名单管理（封禁/解封/查询）和访问日志记录功能，
 * 并定时清理过期的封禁记录。</p>
 */
@Service
public class SecurityService {
    private final IpBlacklistRepository blacklistRepository;
    private final AccessLogRepository accessLogRepository;

    public SecurityService(IpBlacklistRepository blacklistRepository, AccessLogRepository accessLogRepository) {
        this.blacklistRepository = blacklistRepository;
        this.accessLogRepository = accessLogRepository;
    }

    /**
     * 判断指定 IP 是否在黑名单中（未过期）。
     *
     * @param ip IP 地址
     * @return true 表示已被封禁
     */
    public boolean isIpBlocked(String ip) {
        if (ip == null || ip.isBlank()) return false;
        return blacklistRepository.isBlocked(ip, LocalDateTime.now());
    }

    /**
     * 封禁指定 IP。如果该 IP 已在黑名单中，先删除旧记录再创建新记录，避免重复。
     *
     * @param ip     IP 地址
     * @param reason 封禁原因
     * @param hours  封禁时长（小时），为 null 或 <= 0 时永久封禁
     * @return 黑名单记录实体
     */
    @Transactional
    public IpBlacklist blockIp(String ip, String reason, Integer hours) {
        blacklistRepository.findByIpAddress(ip).ifPresent(existing -> {
            blacklistRepository.delete(existing);
        });
        IpBlacklist entry = new IpBlacklist();
        entry.setIpAddress(ip);
        entry.setReason(reason);
        if (hours != null && hours > 0) {
            entry.setBlockedUntil(LocalDateTime.now().plusHours(hours));
            entry.setPermanent(false);
        } else {
            entry.setPermanent(true);
        }
        return blacklistRepository.save(entry);
    }

    /**
     * 解封指定 IP。
     *
     * @param ip IP 地址
     */
    @Transactional
    public void unblockIp(String ip) {
        blacklistRepository.deleteByIpAddress(ip);
    }

    /**
     * 获取全部黑名单记录。
     *
     * @return 黑名单列表
     */
    public List<IpBlacklist> getBlacklist() {
        return blacklistRepository.findAll();
    }

    /**
     * 记录一条访问日志。
     *
     * @param ip         客户端 IP
     * @param method     HTTP 方法
     * @param uri        请求路径
     * @param userAgent  User-Agent
     * @param status     HTTP 状态码
     * @param durationMs 请求耗时（毫秒）
     * @param username   用户名（可为 null）
     * @return 保存后的访问日志实体
     */
    public AccessLog logAccess(String ip, String method, String uri, String userAgent,
                               Integer status, Long durationMs, String username) {
        AccessLog log = new AccessLog();
        log.setIp(ip);
        log.setMethod(method);
        log.setUri(uri);
        log.setUserAgent(userAgent);
        log.setStatus(status);
        log.setDurationMs(durationMs);
        log.setUsername(username);
        return accessLogRepository.save(log);
    }

    /**
     * 分页查询访问日志，支持按 IP 或用户名过滤。
     *
     * @param ip       IP 过滤条件（优先级高于 username）
     * @param username 用户名过滤条件
     * @param pageable 分页参数
     * @return 访问日志分页结果
     */
    public Page<AccessLog> getAccessLogs(String ip, String username, Pageable pageable) {
        if (ip != null) return accessLogRepository.findByIp(ip, pageable);
        if (username != null) return accessLogRepository.findByUsername(username, pageable);
        return accessLogRepository.findAll(pageable);
    }

    /**
     * 定时清理过期的 IP 封禁记录，每小时执行一次。
     */
    @Transactional
    @Scheduled(fixedRate = 3600000) // 每小时执行一次
    public void cleanupExpiredBlocks() {
        blacklistRepository.deleteExpired(LocalDateTime.now());
    }
}
