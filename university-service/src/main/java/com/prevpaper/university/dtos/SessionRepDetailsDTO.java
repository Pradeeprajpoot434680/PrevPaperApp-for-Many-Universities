package com.prevpaper.university.dtos;

import java.time.LocalDateTime;
import java.util.UUID;

public record SessionRepDetailsDTO(
        UUID assignmentId,
        UUID userId,
        String fullName,
        String email,
        UUID sessionId,
        String sessionName, // e.g., "Batch 2022"
        String batchRange,  // e.g., "2022 - 2026"
        LocalDateTime assignedAt,
        Boolean isActive
) {}
