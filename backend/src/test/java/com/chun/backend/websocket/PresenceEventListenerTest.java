package com.chun.backend.websocket;

import com.chun.backend.dto.RoomUsersResponse;
import com.chun.backend.service.PresenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// T38 驗收：訂閱 /topic/room.{roomId} 視為加入、斷線視為離開，人數變動即時推播到 /topic/room.{roomId}.users
@ExtendWith(MockitoExtension.class)
class PresenceEventListenerTest {

    @Mock
    private PresenceService presenceService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private PresenceSessionRegistry sessionRegistry;
    private PresenceEventListener listener;

    @BeforeEach
    void setUp() {
        sessionRegistry = new PresenceSessionRegistry();
        listener = new PresenceEventListener(presenceService, sessionRegistry, messagingTemplate);
    }

    @Test
    void subscribeToRoomTopic_joinsPresence_andBroadcastsOnlineUsers() {
        when(presenceService.getOnlineUsers(1L)).thenReturn(Set.of("alice"));

        listener.handleSubscribe(subscribeEvent("session-1", "/topic/room.1", "alice"));

        verify(presenceService).join(1L, "alice");
        verify(messagingTemplate).convertAndSend(
                eq("/topic/room.1.users"),
                argThatRoomUsers(1L, Set.of("alice"))
        );
    }

    @Test
    void subscribeToUsersTopic_doesNotJoin_butBroadcastsCurrentSnapshot() {
        when(presenceService.getOnlineUsers(1L)).thenReturn(Set.of("bob"));

        listener.handleSubscribe(subscribeEvent("session-1", "/topic/room.1.users", "alice"));

        verify(presenceService, never()).join(any(), any());
        verify(messagingTemplate).convertAndSend(
                eq("/topic/room.1.users"),
                argThatRoomUsers(1L, Set.of("bob"))
        );
    }

    @Test
    void disconnect_afterSubscribe_leavesPresence_andBroadcastsOnlineUsers() {
        when(presenceService.getOnlineUsers(1L)).thenReturn(Set.of("alice"), Set.of());
        listener.handleSubscribe(subscribeEvent("session-1", "/topic/room.1", "alice"));

        listener.handleDisconnect(disconnectEvent("session-1"));

        verify(presenceService).leave(1L, "alice");
        verify(messagingTemplate).convertAndSend(
                eq("/topic/room.1.users"),
                argThatRoomUsers(1L, Set.of())
        );
    }

    @Test
    void disconnect_withoutPriorSubscribe_isNoop() {
        listener.handleDisconnect(disconnectEvent("unknown-session"));

        verify(presenceService, never()).leave(any(), any());
        verifyNoInteractions(messagingTemplate);
    }

    private RoomUsersResponse argThatRoomUsers(Long roomId, Set<String> users) {
        return org.mockito.ArgumentMatchers.argThat(response ->
                response.getRoomId().equals(roomId) && response.getUsers().equals(users));
    }

    private SessionSubscribeEvent subscribeEvent(String sessionId, String destination, String username) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setSessionId(sessionId);
        accessor.setDestination(destination);
        accessor.setUser(new UsernamePasswordAuthenticationToken(username, null, List.of()));
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        return new SessionSubscribeEvent(this, message);
    }

    private SessionDisconnectEvent disconnectEvent(String sessionId) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
        accessor.setSessionId(sessionId);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        return new SessionDisconnectEvent(this, message, sessionId, CloseStatus.NORMAL);
    }
}
