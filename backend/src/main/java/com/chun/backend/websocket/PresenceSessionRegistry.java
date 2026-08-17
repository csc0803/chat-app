package com.chun.backend.websocket;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// STOMP sessionId → 目前所在房間，供斷線時反查該從哪個 online:{roomId} Set 移除
@Component
public class PresenceSessionRegistry {

    private final Map<String, RoomPresence> sessionRooms = new ConcurrentHashMap<>();

    public void register(String sessionId, Long roomId, String username) {
        sessionRooms.put(sessionId, new RoomPresence(roomId, username));
    }

    public RoomPresence remove(String sessionId) {
        return sessionRooms.remove(sessionId);
    }

    public record RoomPresence(Long roomId, String username) {
    }
}
