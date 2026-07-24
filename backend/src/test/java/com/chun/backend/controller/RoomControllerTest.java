package com.chun.backend.controller;

import com.chun.backend.model.Message;
import com.chun.backend.model.Room;
import com.chun.backend.model.User;
import com.chun.backend.repository.MessageRepository;
import com.chun.backend.repository.RoomRepository;
import com.chun.backend.repository.UserRepository;
import com.chun.backend.security.JwtUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

// T34 驗收：GET /api/rooms/{roomId}/messages 需帶 Bearer；回傳 Page metadata
// 順便覆蓋修掉的 LazyInitializationException（Message.room / Message.user 為 LAZY，
// 需靠 MessageResponse DTO 在 session 內轉換完才序列化）
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RoomControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @AfterEach
    void tearDown() {
        messageRepository.deleteAll();
        roomRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void getMessages_withoutToken_isRejected() {
        User user = saveUser("rc_noauth");
        Room room = saveRoom("rc-room-noauth", user);

        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/rooms/" + room.getId() + "/messages", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void getMessages_withValidToken_returnsPageOfMessageResponses() throws Exception {
        User user = saveUser("rc_auth");
        Room room = saveRoom("rc-room-auth", user);
        saveMessage(room, user, "first");
        saveMessage(room, user, "second");

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/rooms/" + room.getId() + "/messages",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders("rc_auth")),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode body = objectMapper.readTree(response.getBody());

        // Page metadata
        assertThat(body.get("totalElements").asLong()).isEqualTo(2);
        assertThat(body.get("content").isArray()).isTrue();
        assertThat(body.get("content")).hasSize(2);

        // 沒有序列化 lazy proxy，而是乾淨的 sender/roomId 純值（本測試覆蓋原本的 500 bug）
        JsonNode first = body.get("content").get(0);
        assertThat(first.get("sender").asText()).isEqualTo("rc_auth");
        assertThat(first.get("roomId").asLong()).isEqualTo(room.getId());
        assertThat(first.has("content")).isTrue();
        assertThat(first.has("sentAt")).isTrue();
    }

    @Test
    void getMessages_ordersByMostRecentFirst() throws Exception {
        User user = saveUser("rc_order");
        Room room = saveRoom("rc-room-order", user);
        saveMessage(room, user, "older");
        Thread.sleep(10);
        saveMessage(room, user, "newer");

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/rooms/" + room.getId() + "/messages",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders("rc_order")),
                String.class);

        JsonNode content = objectMapper.readTree(response.getBody()).get("content");
        assertThat(content.get(0).get("content").asText()).isEqualTo("newer");
        assertThat(content.get(1).get("content").asText()).isEqualTo("older");
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

    private Message saveMessage(Room room, User sender, String content) {
        Message message = new Message();
        message.setRoom(room);
        message.setUser(sender);
        message.setContent(content);
        return messageRepository.save(message);
    }

    private HttpHeaders authHeaders(String username) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtUtil.generateToken(username));
        return headers;
    }
}
