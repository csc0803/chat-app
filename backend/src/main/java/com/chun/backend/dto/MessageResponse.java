package com.chun.backend.dto;

import com.chun.backend.model.Message;
import com.chun.backend.model.MessageType;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class MessageResponse {
    private final Long id;
    private final Long roomId;
    private final String sender;
    private final String content;
    private final MessageType type;
    private final LocalDateTime sentAt;

    private MessageResponse(Long id, Long roomId, String sender, String content,
                             MessageType type, LocalDateTime sentAt) {
        this.id = id;
        this.roomId = roomId;
        this.sender = sender;
        this.content = content;
        this.type = type;
        this.sentAt = sentAt;
    }

    public static MessageResponse from(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getRoom().getId(),
                message.getUser().getUsername(),
                message.getContent(),
                MessageType.CHAT,
                message.getSentAt());
    }
}
