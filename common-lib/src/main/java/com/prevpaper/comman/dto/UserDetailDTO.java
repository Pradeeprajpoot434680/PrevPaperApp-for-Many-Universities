package com.prevpaper.comman.dto;

import java.util.UUID;

public record UserDetailDTO(
        UUID userId,
        String email,
        String fullName
) {}