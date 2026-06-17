package com.chun.backend.controller;

import com.chun.backend.dto.ChangeAdminRequest;
import com.chun.backend.dto.RoomRequest;
import com.chun.backend.model.Room;
import com.chun.backend.service.MessageService;
import com.chun.backend.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;
    private final MessageService messageService;

    // POST /api/rooms
    // body: { "name": "..." }
    // 建立房間，從 JWT 取得 username
    @PostMapping
    public ResponseEntity<?> createRoom(
            @RequestBody RoomRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Room room = roomService.createRoom(request.getName(), userDetails.getUsername());

        return ResponseEntity.status(HttpStatus.CREATED).body(room);
    }

    // PUT /api/rooms/{roomId}/name
    // body: { "name": "..." }
    // 改房間名稱，需 admin
    @PutMapping("/{roomId}/name")
    public ResponseEntity<?> changeRoomName(
            @PathVariable Long roomId,
            @RequestBody RoomRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Room room = roomService.changeRoomName(roomId, request.getName(), userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.OK).body(room);
    }

    // DELETE /api/rooms/{roomId}
    // 軟刪除房間，需 admin
    @DeleteMapping("/{roomId}")
    public ResponseEntity<Void> deleteRoom(
            @PathVariable Long roomId,
            @AuthenticationPrincipal UserDetails userDetails) {
        roomService.deleteRoom(roomId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    // POST /api/rooms/{roomId}/members/{targetUsername}/admin
    // body: { "isAdmin": true/false }
    // 設定 admin 權限，需 admin
    @PostMapping("/{roomId}/members/{targetUsername}/admin")
    public ResponseEntity<Void> changeAdmin(@PathVariable Long roomId,
                                            @PathVariable String targetUsername,
                                            @RequestBody ChangeAdminRequest request,
                                            @AuthenticationPrincipal UserDetails userDetails) {

        roomService.changeAdmin(roomId, targetUsername, request.isAdmin(), userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    // DELETE /api/rooms/{roomId}/members/me
    // 離開房間，從 JWT 取得 username
    @DeleteMapping("/{roomId}/members/me")
    public ResponseEntity<Void> leaveRoom(@PathVariable Long roomId,
                                          @AuthenticationPrincipal UserDetails userDetails) {
        roomService.leaveRoom(roomId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    // GET /api/rooms/{roomId}/messages?page=0&size=20
    // 取歷史訊息，需 Bearer，回傳 Page metadata
    @GetMapping("/{roomId}/messages")
    public ResponseEntity<?> getMessages(
            @PathVariable Long roomId,
            @PageableDefault(size = 20, sort = "sentAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<?> messages = messageService.getMessages(roomId, pageable);
        return ResponseEntity.ok(messages);
    }
}
