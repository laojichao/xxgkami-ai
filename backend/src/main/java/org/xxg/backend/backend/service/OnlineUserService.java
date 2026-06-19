package org.xxg.backend.backend.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 在线用户管理服务
 * 基于内存ConcurrentHashMap追踪在线用户状态，30分钟无活动自动判定为离线
 * 提供用户上线/下线状态管理和在线用户列表查询功能
 * <p><b>架构限制：</b>当前使用内存存储（ConcurrentHashMap），仅支持单实例部署。
 * 多实例部署时会导致各实例在线用户状态不一致。
 * TODO: 未来需迁移到 Redis 等分布式缓存，使用 SETEX 命令存储用户在线状态，
 * 并通过 Redis 的 PUB/SUB 或键空间通知实现跨实例状态同步。</p>
 */
@Service
public class OnlineUserService {
    private final Map<String, Long> onlineUsers = new ConcurrentHashMap<>();

    public void userOnline(String username) {
        onlineUsers.put(username, System.currentTimeMillis());
    }

    public void userOffline(String username) {
        onlineUsers.remove(username);
    }

    public boolean isOnline(String username) {
        Long lastSeen = onlineUsers.get(username);
        if (lastSeen == null) return false;
        // Consider offline after 30 minutes of inactivity
        return System.currentTimeMillis() - lastSeen < 30 * 60 * 1000;
    }

    @Scheduled(fixedRate = 300000)
    public void cleanupStaleEntries() {
        long now = System.currentTimeMillis();
        onlineUsers.entrySet().removeIf(e -> now - e.getValue() > 30 * 60 * 1000);
    }

    public int getOnlineCount() {
        return onlineUsers.size();
    }

    public List<String> getOnlineUsers() {
        return new ArrayList<>(onlineUsers.keySet());
    }
}
