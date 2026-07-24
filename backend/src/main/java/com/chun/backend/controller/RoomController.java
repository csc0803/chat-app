package com.chun.backend.controller;

import com.chun.backend.dto.ChangeAdminRequest;
import com.chun.backend.dto.MessageResponse;
import com.chun.backend.dto.RoomRequest;
import com.chun.backend.dto.RoomResponse;
import com.chun.backend.service.MessageService;
import com.chun.backend.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;
    private final MessageService messageService;

    // GET /api/rooms
    // 列出目前使用者所屬的房間
    @GetMapping
    public ResponseEntity<List<RoomResponse>> getRooms(Authentication authentication) {
        List<RoomResponse> rooms = roomService.getRoomsForUser(authentication.getName());
        return ResponseEntity.ok(rooms);
    }

    // POST /api/rooms
    // body: { "name": "..." }
    // 建立房間，從 JWT 取得 username
    @PostMapping
    public ResponseEntity<RoomResponse> createRoom(
            @RequestBody RoomRequest request,
            Authentication authentication) {

        RoomResponse room = roomService.createRoom(request.getName(), authentication.getName());

        return ResponseEntity.status(HttpStatus.CREATED).body(room);
    }

    // PUT /api/rooms/{roomId}/name
    // body: { "name": "..." }
    // 改房間名稱，需 admin
    @PutMapping("/{roomId}/name")
    public ResponseEntity<RoomResponse> changeRoomName(
            @PathVariable Long roomId,
            @RequestBody RoomRequest request,
            Authentication authentication) {
        RoomResponse room = roomService.changeRoomName(roomId, request.getName(), authentication.getName());
        return ResponseEntity.status(HttpStatus.OK).body(room);
    }

    // DELETE /api/rooms/{roomId}
    // 軟刪除房間，需 admin
    @DeleteMapping("/{roomId}")
    public ResponseEntity<Void> deleteRoom(
            @PathVariable Long roomId,
            Authentication authentication) {
        roomService.deleteRoom(roomId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    // POST /api/rooms/{roomId}/members/{targetUsername}/admin
    // body: { "isAdmin": true/false }
    // 設定 admin 權限，需 admin
    @PostMapping("/{roomId}/members/{targetUsername}/admin")
    public ResponseEntity<Void> changeAdmin(@PathVariable Long roomId,
                                            @PathVariable String targetUsername,
                                            @RequestBody ChangeAdminRequest request,
                                            Authentication authentication) {

        roomService.changeAdmin(roomId, targetUsername, request.isAdmin(), authentication.getName());
        return ResponseEntity.noContent().build();
    }

    // DELETE /api/rooms/{roomId}/members/me
    // 離開房間，從 JWT 取得 username
    @DeleteMapping("/{roomId}/members/me")
    public ResponseEntity<Void> leaveRoom(@PathVariable Long roomId,
                                          Authentication authentication) {
        roomService.leaveRoom(roomId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    // GET /api/rooms/{roomId}/messages?page=0&size=20
    // 取歷史訊息，需 Bearer，回傳 Page metadata
    @GetMapping("/{roomId}/messages")
    public ResponseEntity<Page<MessageResponse>> getMessages(
            @PathVariable Long roomId,
            @PageableDefault(size = 20, sort = "sentAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<MessageResponse> messages = messageService.getMessages(roomId, pageable);
        return ResponseEntity.ok(messages);
    }
}
