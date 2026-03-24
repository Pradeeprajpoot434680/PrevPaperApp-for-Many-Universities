package com.prevpaper.comman.dto;

import java.util.List;

public record AuthResponse(
        boolean isValid,
        boolean isVerified,
        List<String> roles,
        String universityId,
        String userId,
        String email,
        String scopeId
) {
}
