package com.chun.backend.service;

import com.chun.backend.exception.UserNotFoundException;
import com.chun.backend.model.Room;
import com.chun.backend.model.RoomMember;
import com.chun.backend.model.RoomMemberId;
import com.chun.backend.model.User;
import com.chun.backend.repository.RoomMemberRepository;
import com.chun.backend.repository.RoomRepository;
import com.chun.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final UserRepository userRepository;

    @Transactional
    public Room createRoom(String roomName, String username) {
        // 1. 查 user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));

        // 2. 建立並儲存 Room（creator = user）
        Room room = new Room();
        room.setName(roomName);
        room.setCreator(user);
        room = roomRepository.save(room);

        // 3. 建立 RoomMember（admin = true），寫入 DB
        RoomMember roomMember = new RoomMember();
        roomMember.setRoom(room);
        roomMember.setUser(user);
        roomMember.setId(new RoomMemberId());
        roomMember.setAdmin(true);
        roomMemberRepository.save(roomMember);

        // 4. 回傳 room
        return room;
    }
}
