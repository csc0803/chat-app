package com.chun.backend.dto;

import com.chun.backend.model.Room;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class RoomResponse {
    private final Long id;
    private final String name;
    private final String creatorUsername;
    private final LocalDateTime createdAt;

    private RoomResponse(Long id, String name, String creatorUsername, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.creatorUsername = creatorUsername;
        this.createdAt = createdAt;
    }

    public static RoomResponse from(Room room) {
        return new RoomResponse(
                room.getId(),
                room.getName(),
                room.getCreator().getUsername(),
                room.getCreatedAt());
    }
}
