package com.chun.backend.service;

import com.chun.backend.exception.RoomNotFoundException;
import com.chun.backend.exception.UserNotFoundException;
import com.chun.backend.model.Room;
import com.chun.backend.model.RoomMember;
import com.chun.backend.model.RoomMemberId;
import com.chun.backend.model.User;
import com.chun.backend.repository.RoomMemberRepository;
import com.chun.backend.repository.RoomRepository;
import com.chun.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;


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

    @Transactional
    public Room changeRoomName(Long roomId, String newRoomName, String username) {
        // 1. 查 user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));

        // 2. 查room
        Room room = roomRepository.findByIdAndIsDeletedFalse(roomId)
                .orElseThrow(() -> new RoomNotFoundException(roomId));

        // 3. 查 RoomMember 確認是 admin（不是就拋例外）
        RoomMemberId roomMemberId = new RoomMemberId();
        roomMemberId.setUserId(user.getId());
        roomMemberId.setRoomId(roomId);

        RoomMember roomMember = roomMemberRepository.findByIdAndIsDeletedFalse(roomMemberId)
                .orElseThrow(() -> new AccessDeniedException("不是此房間成員"));

        if (!roomMember.isAdmin()) {
            throw new AccessDeniedException("沒有管理員權限");
        }

        room.setName(newRoomName);
        room = roomRepository.save(room);

        return room;
    }

    @Transactional
    public void deleteRoom(Long roomId, String username) {
        // 1. 查 user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));

        // 2. 查 room（找不到或已刪除就拋 RoomNotFoundException）
        Room room = roomRepository.findByIdAndIsDeletedFalse(roomId)
                .orElseThrow(() -> new RoomNotFoundException(roomId));

        // 3. 查 RoomMember 確認是 admin（不是就拋例外）
        RoomMemberId roomMemberId = new RoomMemberId();
        roomMemberId.setUserId(user.getId());
        roomMemberId.setRoomId(roomId);

        RoomMember roomMember = roomMemberRepository.findByIdAndIsDeletedFalse(roomMemberId)
                .orElseThrow(() -> new AccessDeniedException("不是此房間成員"));

        if (!roomMember.isAdmin()) {
            throw new AccessDeniedException("沒有管理員權限");
        }

        // 4. 軟刪除：setIsDeleted(true)、setDeletedAt(LocalDateTime.now())
        room.setDeleted(true);
        room.setDeletedAt(LocalDateTime.now());

        // 5. save
        room = roomRepository.save(room);
    }

    @Transactional
    public void changeAdmin(Long roomId, String targetUsername, boolean isAdmin, String username) {

        // 1. 查 user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));

        // 2. 查 room（找不到或已刪除就拋 RoomNotFoundException）
        Room room = roomRepository.findByIdAndIsDeletedFalse(roomId)
                .orElseThrow(() -> new RoomNotFoundException(roomId));

        // 3. 查 RoomMember 確認是 admin（不是就拋例外）
        RoomMemberId roomMemberId = new RoomMemberId();
        roomMemberId.setUserId(user.getId());
        roomMemberId.setRoomId(roomId);

        RoomMember roomMember = roomMemberRepository.findByIdAndIsDeletedFalse(roomMemberId)
                .orElseThrow(() -> new AccessDeniedException("不是此房間成員"));

        if (!roomMember.isAdmin()) {
            throw new AccessDeniedException("沒有管理員權限");
        }

        // 4. 查 target user
        User targetUser = userRepository.findByUsername(targetUsername)
                .orElseThrow(() -> new UserNotFoundException(targetUsername));

        // 5. 查 target RoomMember（不是成員就拋例外）

        RoomMemberId targetRoomMemberId = new RoomMemberId();
        targetRoomMemberId.setUserId(targetUser.getId());
        targetRoomMemberId.setRoomId(roomId);

        RoomMember targetRoomMember = roomMemberRepository.findByIdAndIsDeletedFalse(targetRoomMemberId)
                .orElseThrow(() -> new AccessDeniedException("不是此房間成員"));

        // 6. setAdmin(isAdmin)
        targetRoomMember.setAdmin(isAdmin);

        // 7. save
        targetRoomMember = roomMemberRepository.save(targetRoomMember);
    }

    @Transactional
    public void leaveRoom(Long roomId, String username) {
        // 1. 查 user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));

        // 2. 查 room（找不到或已刪除就拋 RoomNotFoundException）
        Room room = roomRepository.findByIdAndIsDeletedFalse(roomId)
                .orElseThrow(() -> new RoomNotFoundException(roomId));

        // 3. 查 RoomMember（不是成員就拋例外）
        RoomMemberId roomMemberId = new RoomMemberId();
        roomMemberId.setUserId(user.getId());
        roomMemberId.setRoomId(roomId);

        RoomMember roomMember = roomMemberRepository.findByIdAndIsDeletedFalse(roomMemberId)
                .orElseThrow(() -> new AccessDeniedException("不是此房間成員"));

        // 4. 若為最後一個 admin → 先轉移管理員權限
        if (roomMember.isAdmin() && roomMemberRepository.countByRoom_IdAndIsAdminTrueAndIsDeletedFalse(roomId) == 1) {
            RoomMember newAdmin = roomMemberRepository
                    .findFirstByRoom_IdAndIsAdminFalseAndIsDeletedFalseOrderByJoinedAtAsc(roomId)
                    .orElseThrow(() -> new AccessDeniedException("查無其他成員"));

            changeAdmin(roomId, newAdmin.getUser().getUsername(), true, user.getUsername());
        }

        // 5. 刪除 RoomMember
        removeRoomMember(roomId, user.getId());

    }

    @Transactional
    public void removeRoomMember(Long roomId, Long userId) {
        // 1. 組 RoomMemberId
        RoomMemberId roomMemberId = new RoomMemberId();
        roomMemberId.setUserId(userId);
        roomMemberId.setRoomId(roomId);

        // 2. 查 RoomMember（找不到就拋例外）
        RoomMember roomMember = roomMemberRepository.findByIdAndIsDeletedFalse(roomMemberId)
                .orElseThrow(() -> new AccessDeniedException("不是此房間成員"));

        // 3. 刪除 RoomMember
        roomMember.setDeleted(true);
        roomMember.setDeletedAt(LocalDateTime.now());
        roomMemberRepository.save(roomMember);

        // 若房間已無 active 成員，自動軟刪除房間
        if (roomMemberRepository.countByRoom_IdAndIsDeletedFalse(roomId) == 0) {
            Room room = roomRepository.findByIdAndIsDeletedFalse(roomId)
                    .orElseThrow(() -> new RoomNotFoundException(roomId));
            room.setDeleted(true);
            room.setDeletedAt(LocalDateTime.now());
            roomRepository.save(room);
        }
    }
}
