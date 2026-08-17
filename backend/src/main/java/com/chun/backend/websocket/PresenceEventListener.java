package com.chun.backend.websocket;

import com.chun.backend.dto.RoomUsersResponse;
import com.chun.backend.service.PresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// 訂閱 /topic/room.{roomId}（不含 .users）視為加入房間，斷線視為離開；
// 訂閱 /topic/room.{roomId}.users 本身不算加入，但會補發一次目前名單快照——
// 避免「剛加入房間的人在別人變動之前看不到自己」的競態（見 T38 除錯記錄）
@Component
@RequiredArgsConstructor
public class PresenceEventListener {

    private static final Pattern ROOM_TOPIC = Pattern.compile("^/topic/room\\.(\\d+)$");
    private static final Pattern ROOM_USERS_TOPIC = Pattern.compile("^/topic/room\\.(\\d+)\\.users$");

    private final PresenceService presenceService;
    private final PresenceSessionRegistry sessionRegistry;
    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = accessor.getDestination();
        if (destination == null || accessor.getUser() == null) {
            return;
        }

        Matcher usersMatcher = ROOM_USERS_TOPIC.matcher(destination);
        if (usersMatcher.matches()) {
            broadcastOnlineUsers(Long.valueOf(usersMatcher.group(1)));
            return;
        }

        Matcher matcher = ROOM_TOPIC.matcher(destination);
        if (!matcher.matches()) {
            return;
        }

        Long roomId = Long.valueOf(matcher.group(1));
        String username = accessor.getUser().getName();

        presenceService.join(roomId, username);
        sessionRegistry.register(accessor.getSessionId(), roomId, username);
        broadcastOnlineUsers(roomId);
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        PresenceSessionRegistry.RoomPresence presence = sessionRegistry.remove(event.getSessionId());
        if (presence == null) {
            return;
        }

        presenceService.leave(presence.roomId(), presence.username());
        broadcastOnlineUsers(presence.roomId());
    }

    private void broadcastOnlineUsers(Long roomId) {
        Set<String> users = presenceService.getOnlineUsers(roomId);
        messagingTemplate.convertAndSend(
                "/topic/room." + roomId + ".users",
                new RoomUsersResponse(roomId, users)
        );
    }
}
