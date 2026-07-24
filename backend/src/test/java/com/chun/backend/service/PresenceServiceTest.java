package com.chun.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// T37 驗收：JOIN 加入 / LEAVE 或斷線移除；heartbeat 維持 TTL
// 純 Mockito 單元測試，不依賴真實 Redis（本機/CI 沒有 Redis server 可連）
@ExtendWith(MockitoExtension.class)
class PresenceServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private SetOperations<String, String> setOperations;

    private PresenceService presenceService;

    @BeforeEach
    void setUp() {
        presenceService = new PresenceService(redisTemplate);
    }

    @Test
    void join_addsUsernameToRoomSet_andRefreshesTtl() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);

        presenceService.join(1L, "alice");

        verify(setOperations).add("online:1", "alice");
        verify(redisTemplate).expire(eq("online:1"), any(Duration.class));
    }

    @Test
    void leave_removesUsernameFromRoomSet() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);

        presenceService.leave(1L, "alice");

        verify(setOperations).remove("online:1", "alice");
    }

    @Test
    void heartbeat_refreshesTtl() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);

        presenceService.heartbeat(1L, "alice");

        verify(setOperations).add("online:1", "alice");
        verify(redisTemplate).expire(eq("online:1"), any(Duration.class));
    }

    @Test
    void getOnlineUsers_returnsSetMembers() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members("online:1")).thenReturn(Set.of("alice", "bob"));

        Set<String> online = presenceService.getOnlineUsers(1L);

        assertThat(online).containsExactlyInAnyOrder("alice", "bob");
    }

    @Test
    void getOnlineUsers_whenKeyMissing_returnsEmptySet() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members("online:1")).thenReturn(null);

        Set<String> online = presenceService.getOnlineUsers(1L);

        assertThat(online).isEmpty();
    }
}
