package com.prevpaper.comman.dto;

import java.util.UUID;

public record UserData(
        UUID userId,
        String firstName,
        String lastName,
        String profileImageUrl
) {
}
