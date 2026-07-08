package com.chun.backend.dto;

import com.chun.backend.model.MessageType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatMessage {
    private Long roomId;
    private String content;
    private MessageType type;
}
