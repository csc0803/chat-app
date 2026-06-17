package com.chun.backend.exception;

public class RoomNotFoundException extends RuntimeException {
    public RoomNotFoundException(Long roomId) {
        super("Username : " + roomId + " not found");
    }
}
