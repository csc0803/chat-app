package com.chun.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Set;

@Getter
@AllArgsConstructor
public class RoomUsersResponse {
    private Long roomId;
    private Set<String> users;
}
