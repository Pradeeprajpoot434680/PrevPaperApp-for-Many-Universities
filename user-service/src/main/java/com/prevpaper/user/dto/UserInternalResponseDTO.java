package com.prevpaper.user.dto;

import java.io.Serializable;
import java.util.UUID;

public record UserInternalResponseDTO(
        UUID id,
        UUID authUserId,
        String firstName,
        String lastName,
        String profileImageUrl,
        String bio
) implements Serializable {
    private static final long serialVersionUID = 1L;
}