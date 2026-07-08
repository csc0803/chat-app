package com.chun.backend.service;

import com.chun.backend.model.Message;
import com.chun.backend.model.Room;
import com.chun.backend.model.User;
import com.chun.backend.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;

    public Page<Message> getMessages(Long roomId, Pageable pageable) {
        return messageRepository.findByRoomIdOrderBySentAtDesc(roomId, pageable);
    }

    public Message save(User user, Room room, String content) {
        Message message = new Message();
        message.setUser(user);
        message.setRoom(room);
        message.setContent(content);
        return messageRepository.save(message);
    }
}