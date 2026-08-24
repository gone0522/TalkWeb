package com.talkweb.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class PresenceManager {

    private final SimpMessagingTemplate messagingTemplate;
    // Map of userId -> Set of Session IDs
    private final Map<Long, Set<String>> onlineUserSessions = new ConcurrentHashMap<>();
    // Reverse map of sessionId -> userId
    private final Map<String, Long> sessionToUserMap = new ConcurrentHashMap<>();

    public PresenceManager(@Lazy SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public synchronized void registerSession(Long userId, String sessionId) {
        sessionToUserMap.put(sessionId, userId);
        onlineUserSessions.computeIfAbsent(userId, k -> Collections.synchronizedSet(new HashSet<>())).add(sessionId);

        log.debug("使用者 ID {} 上線 (Session: {}), 目前在線裝置數: {}", userId, sessionId, onlineUserSessions.get(userId).size());
        broadcastPresence(userId, true);
    }

    public synchronized void unregisterSession(String sessionId) {
        Long userId = sessionToUserMap.remove(sessionId);
        if (userId != null) {
            Set<String> sessions = onlineUserSessions.get(userId);
            if (sessions != null) {
                sessions.remove(sessionId);
                if (sessions.isEmpty()) {
                    onlineUserSessions.remove(userId);
                    log.debug("使用者 ID {} 所有連線已中斷 (離線)", userId);
                    broadcastPresence(userId, false);
                }
            }
        }
    }

    public boolean isUserOnline(Long userId) {
        Set<String> sessions = onlineUserSessions.get(userId);
        return sessions != null && !sessions.isEmpty();
    }

    public Set<Long> getOnlineUserIds() {
        return new HashSet<>(onlineUserSessions.keySet());
    }

    private void broadcastPresence(Long userId, boolean online) {
        try {
            Map<String, Object> payload = Map.of(
                    "userId", userId,
                    "online", online
            );
            messagingTemplate.convertAndSend("/topic/presence", payload);
        } catch (Exception e) {
            log.error("推播在線狀態失敗: {}", e.getMessage());
        }
    }
}
