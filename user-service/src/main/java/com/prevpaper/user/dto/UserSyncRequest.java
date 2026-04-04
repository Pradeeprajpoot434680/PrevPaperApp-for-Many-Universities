package com.prevpaper.user.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class UserSyncRequest {
    private UUID authUserId;
    private String email;
    private String firstName;
    private String lastName;
}