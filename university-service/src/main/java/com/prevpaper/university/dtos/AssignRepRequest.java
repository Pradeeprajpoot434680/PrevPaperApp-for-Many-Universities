package com.prevpaper.university.dtos;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class AssignRepRequest {
    private UUID userId;
    private UUID scopeId; // This will be the University ID [cite: 28, 29]
    private LocalDateTime expiresAt;
}
