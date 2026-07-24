package com.chun.backend.service;

import com.chun.backend.dto.MessageResponse;
import com.chun.backend.model.Message;
import com.chun.backend.model.Room;
import com.chun.backend.model.User;
import com.chun.backend.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;

    @Transactional(readOnly = true)
    public Page<MessageResponse> getMessages(Long roomId, Pageable pageable) {
        return messageRepository.findByRoomIdOrderBySentAtDesc(roomId, pageable)
                .map(MessageResponse::from);
    }

    public Message save(User user, Room room, String content) {
        Message message = new Message();
        message.setUser(user);
        message.setRoom(room);
        message.setContent(content);
        return messageRepository.save(message);
    }
}