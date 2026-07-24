package com.chun.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;

// Redis key: online:{roomId} — Set，存放在線 username
// key 本身掛 TTL，JOIN / heartbeat 時續命；斷線沒收到訊息時靠 TTL 過期自然清掉殘留
@Service
@RequiredArgsConstructor
public class PresenceService {

    private static final Duration ONLINE_TTL = Duration.ofSeconds(60);

    private final StringRedisTemplate redisTemplate;

    public void join(Long roomId, String username) {
        String key = onlineKey(roomId);
        redisTemplate.opsForSet().add(key, username);
        redisTemplate.expire(key, ONLINE_TTL);
    }

    public void leave(Long roomId, String username) {
        redisTemplate.opsForSet().remove(onlineKey(roomId), username);
    }

    public void heartbeat(Long roomId, String username) {
        String key = onlineKey(roomId);
        redisTemplate.opsForSet().add(key, username);
        redisTemplate.expire(key, ONLINE_TTL);
    }

    public Set<String> getOnlineUsers(Long roomId) {
        Set<String> members = redisTemplate.opsForSet().members(onlineKey(roomId));
        return members != null ? members : Set.of();
    }

    private String onlineKey(Long roomId) {
        return "online:" + roomId;
    }
}
