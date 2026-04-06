package com.prevpaper.university.dtos;

import java.time.LocalDateTime;
import java.util.UUID;

public record RepresentativeDetailsDTO(
        UUID assignmentId,
        UUID userId,
        String fullName,
        String email,
        UUID universityId,
        String universityName,
        String universityCode,
        LocalDateTime assignedAt,
        Boolean isActive
) {}