package com.chun.backend.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String username) {
        super("Username : " + username + " not found");
    }
}
