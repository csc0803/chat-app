package com.chun.backend.dto;

import com.chun.backend.model.MessageType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ChatResponse {
    private Long roomId;
    private String sender;
    private String content;
    private MessageType type;
    private LocalDateTime sentAt;
}
