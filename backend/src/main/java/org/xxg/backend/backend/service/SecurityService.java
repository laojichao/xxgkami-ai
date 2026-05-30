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

@Service
public class SecurityService {
    private final IpBlacklistRepository blacklistRepository;
    private final AccessLogRepository accessLogRepository;

    public SecurityService(IpBlacklistRepository blacklistRepository, AccessLogRepository accessLogRepository) {
        this.blacklistRepository = blacklistRepository;
        this.accessLogRepository = accessLogRepository;
    }

    public boolean isIpBlocked(String ip) {
        return blacklistRepository.isBlocked(ip, LocalDateTime.now());
    }

    public IpBlacklist blockIp(String ip, String reason, Integer hours) {
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

    public void unblockIp(String ip) {
        blacklistRepository.findByIpAddress(ip).ifPresent(blacklistRepository::delete);
    }

    public List<IpBlacklist> getBlacklist() {
        return blacklistRepository.findAll();
    }

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

    public Page<AccessLog> getAccessLogs(String ip, String username, Pageable pageable) {
        if (ip != null) return accessLogRepository.findByIp(ip, pageable);
        if (username != null) return accessLogRepository.findByUsername(username, pageable);
        return accessLogRepository.findAll(pageable);
    }

    @Transactional
    @Scheduled(fixedRate = 3600000) // Every hour
    public void cleanupExpiredBlocks() {
        blacklistRepository.deleteExpired(LocalDateTime.now());
    }
}
