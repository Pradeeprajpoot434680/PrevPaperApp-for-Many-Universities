package com.prevpaper.comman.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class UserInternalInfoDTO {
    private UUID userId;
    private String email;
}