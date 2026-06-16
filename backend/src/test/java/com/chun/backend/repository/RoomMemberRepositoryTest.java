package com.chun.backend.repository;

import com.chun.backend.model.Room;
import com.chun.backend.model.RoomMember;
import com.chun.backend.model.RoomMemberId;
import com.chun.backend.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class RoomMemberRepositoryTest {

    @Autowired
    private RoomMemberRepository roomMemberRepository;
    @Autowired
    private TestEntityManager em;

    private User user;
    private Room room;

    @BeforeEach
    void setUp() {
        // 建立並持久化 user、room
        user = new User();
        user.setUsername("user1");
        user.setPasswordHash("password1");
        room = new Room();
        room.setName("room1");
        room.setCreator(user);

        em.persist(user);
        em.persist(room);
    }

    private RoomMember buildMember(User u, Room r, boolean isAdmin) {
        // 組裝 RoomMemberId + RoomMember
        RoomMember roomMember = new RoomMember();
        roomMember.setRoom(r);
        roomMember.setUser(u);
        roomMember.setId(new RoomMemberId());
        roomMember.setAdmin(isAdmin);

        return roomMember;
    }

    @Test
    void save_findById_pass() {
        RoomMember roomMember = buildMember(user, room, true);
        em.persist(roomMember);
        em.flush();

        Optional<RoomMember> result =  roomMemberRepository.findById(roomMember.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getUser().getUsername()).isEqualTo(user.getUsername());
    }

    @Test
    void findById_notExist_returnsEmpty() {
        RoomMemberId key = new RoomMemberId();
        key.setRoomId(999L);
        key.setUserId(999L);

        Optional<RoomMember> result =  roomMemberRepository.findById(key);

        assertThat(result).isNotPresent();
    }

    @Test
    void existsById_pass() {
        RoomMember roomMember = buildMember(user, room, true);
        em.persist(roomMember);
        em.flush();

        RoomMemberId key = new RoomMemberId();
        key.setRoomId(room.getId());
        key.setUserId(user.getId());

        assertThat(roomMemberRepository.existsById(key)).isTrue();

    }

    @Test
    void delete_removeMembership() {
        RoomMember roomMember = buildMember(user, room, true);
        em.persist(roomMember);
        em.flush();

        roomMemberRepository.deleteById(roomMember.getId());
        Optional<RoomMember> result = roomMemberRepository.findById(roomMember.getId());
        assertThat(result).isNotPresent();
    }

    @Test
    void isAdmin_defaultFalse() {
        RoomMember roomMember = new RoomMember();
        roomMember.setUser(user);
        roomMember.setRoom(room);
        roomMember.setId(new RoomMemberId());
        em.persist(roomMember);
        em.flush();

        Optional<RoomMember> result = roomMemberRepository.findById(roomMember.getId());
        assertThat(result.get().isAdmin()).isFalse();
    }

    @Test
    void joinedAt_autoSet() {
        RoomMember roomMember = buildMember(user, room, true);
        em.persist(roomMember);
        em.flush();

        Optional<RoomMember> result = roomMemberRepository.findById(roomMember.getId());
        assertThat(result.get().getJoinedAt()).isNotNull();
    }
}