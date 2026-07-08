package com.chun.backend;

import com.chun.backend.dto.ChatMessage;
import com.chun.backend.dto.ChatResponse;
import com.chun.backend.model.MessageType;
import com.chun.backend.model.Room;
import com.chun.backend.model.User;
import com.chun.backend.repository.MessageRepository;
import com.chun.backend.repository.RoomRepository;
import com.chun.backend.repository.UserRepository;
import com.chun.backend.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ChatWebSocketTest {

    @LocalServerPort
    private int port;

    @Autowired private JwtUtil jwtUtil;
    @Autowired private UserRepository userRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private MessageRepository messageRepository;

    @MockitoSpyBean
    private SimpMessagingTemplate messagingTemplate;

    private WebSocketStompClient stompClient;
    private StompSession activeSession;

    @BeforeEach
    void setUp() {
        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(mapper);
        stompClient.setMessageConverter(converter);
    }

    @AfterEach
    void tearDown() {
        if (activeSession != null && activeSession.isConnected()) {
            activeSession.disconnect();
        }
        messageRepository.deleteAll();
        roomRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ── T27 驗收 1：無 token 拒連 ──────────────────────────────────────────
    @Test
    void connect_withoutToken_shouldBeRejected() {
        CompletableFuture<StompSession> future = stompClient.connectAsync(
                "ws://localhost:" + port + "/ws-stomp",
                new StompSessionHandlerAdapter() {}
        );
        assertThrows(ExecutionException.class, () -> future.get(5, TimeUnit.SECONDS));
    }

    // ── T27 驗收 2：有效 token 連線成功 ────────────────────────────────────
    @Test
    void connect_withValidToken_sessionIsConnected() throws Exception {
        saveUser("wstest_conn");
        activeSession = connectWithToken("wstest_conn");
        assertThat(activeSession.isConnected()).isTrue();
    }

    // ── T27 驗收 3：有效 token 可發訊息，server 持久化並廣播 ─────────────
    @Test
    void send_withValidToken_persistsMessage_andBroadcasts() throws Exception {
        User user = saveUser("wstest");
        Room room = saveRoom("test-room", user);

        activeSession = connectWithToken("wstest");
        assertThat(activeSession.isConnected()).isTrue();

        Thread.sleep(300);

        ChatMessage msg = new ChatMessage();
        msg.setRoomId(room.getId());
        msg.setContent("hello");
        msg.setType(MessageType.CHAT);
        activeSession.send("/app/chat.send", msg);

        // 驗收 1：訊息已存入 DB
        assertThat(messageRepository.count())
                .as("ChatController 應已將訊息儲存至 DB")
                .isEqualTo(1);

        // 驗收 2：messagingTemplate.convertAndSend 有被呼叫並帶正確參數
        ArgumentCaptor<ChatResponse> captor = ArgumentCaptor.forClass(ChatResponse.class);
        verify(messagingTemplate, timeout(2000))
                .convertAndSend(eq("/topic/room." + room.getId()), any(ChatResponse.class));
    }

    // ── helper ─────────────────────────────────────────────────────────────

    private User saveUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash("hash");
        return userRepository.save(user);
    }

    private Room saveRoom(String name, User creator) {
        Room room = new Room();
        room.setName(name);
        room.setCreator(creator);
        return roomRepository.save(room);
    }

    private StompSession connectWithToken(String username) throws Exception {
        String token = jwtUtil.generateToken(username);
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + token);
        return stompClient.connectAsync(
                "ws://localhost:" + port + "/ws-stomp",
                new WebSocketHttpHeaders(),
                connectHeaders,
                new StompSessionHandlerAdapter() {}
        ).get(5, TimeUnit.SECONDS);
    }
}
