package com.chun.backend.repository;

import com.chun.backend.model.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, Long> {
    Page<Message> findByRoomIdOrderBySentAtDesc(Long roomId, Pageable pageable);
}
