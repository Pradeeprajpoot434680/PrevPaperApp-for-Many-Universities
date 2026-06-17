package com.prevpaper.user.dto;

import com.prevpaper.comman.enums.AccountStatus;
import java.io.Serializable;
import java.util.UUID;

public record AccountResponseDTO(
        UUID id,
        UUID userId,
        UUID universityId,
        UUID departmentId,
        UUID programId,
        String batchYear,
        Long totalPoints,
        AccountStatus status
) implements Serializable {
    private static final long serialVersionUID = 1L;
}