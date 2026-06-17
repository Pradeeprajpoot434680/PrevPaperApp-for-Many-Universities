package com.prevpaper.university.dtos;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

public record RepresentativeDetailsDTO(
        UUID id,
        UUID userId,
        String fullName,
        String email,
        UUID universityId,
        String universityName,
        String universityCode,
        String assignedAt,
        Boolean isActive
) implements Serializable {
    private static final long serialVersionUID = 1L;
}