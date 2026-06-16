package com.chun.backend.repository;

import com.chun.backend.model.Message;
import com.chun.backend.model.Room;
import com.chun.backend.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;


@DataJpaTest
public class MessageRepositoryTest {
    @Autowired
    MessageRepository messageRepository;
    @Autowired
    TestEntityManager em;

    @Test
    void test() {

        User user1 = new User();
        user1.setUsername("user1");
        user1.setPasswordHash("password1");

        Room room1 = new Room();
        room1.setName("test1");
        room1.setCreator(user1);

        Message message1 = new Message();
        message1.setRoom(room1);
        message1.setUser(user1);
        message1.setContent("hello");
        message1.setSentAt(LocalDateTime.now().minusSeconds(1));

        Message message2 = new Message();
        message2.setRoom(room1);
        message2.setUser(user1);
        message2.setContent("world");
        message2.setSentAt(LocalDateTime.now());

        em.persist(user1);   // 先存 user
        em.persist(room1);   // room 需要 user(creator),所以 user 要先存
        em.persist(message1);
        em.persist(message2);
        em.flush();

        Page<Message> result = messageRepository.findByRoomIdOrderBySentAtDesc(room1.getId(), PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getUser().getUsername()).isEqualTo("user1");
        assertThat(result.getContent().get(0).getRoom().getName()).isEqualTo("test1");

        assertThat(result.getContent())
                .extracting(Message::getContent)
                .containsExactly("world", "hello");


    }
}
