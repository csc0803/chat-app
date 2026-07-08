package com.chun.backend.controller;

import com.chun.backend.dto.ChatMessage;
import com.chun.backend.dto.ChatResponse;
import com.chun.backend.model.Message;
import com.chun.backend.model.Room;
import com.chun.backend.model.User;
import com.chun.backend.repository.RoomRepository;
import com.chun.backend.repository.UserRepository;
import com.chun.backend.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageService messageService;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;

    @MessageMapping("/chat.send")
    public void send(ChatMessage chatMessage, Principal principal) {
        String username = principal.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Room room = roomRepository.findByIdAndIsDeletedFalse(chatMessage.getRoomId())
                .orElseThrow(() -> new RuntimeException("Room not found"));

        Message saved = messageService.save(user, room, chatMessage.getContent());

        ChatResponse response = new ChatResponse(
                room.getId(),
                username,
                saved.getContent(),
                chatMessage.getType(),
                saved.getSentAt()
        );

        messagingTemplate.convertAndSend("/topic/room." + room.getId(), response);
    }
}
