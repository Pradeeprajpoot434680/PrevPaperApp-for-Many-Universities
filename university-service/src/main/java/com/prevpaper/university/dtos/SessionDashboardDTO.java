package com.prevpaper.university.dtos;

import java.util.UUID;

public record SessionDashboardDTO(
        UUID sessionId,
        String name,
        String batchRange, // e.g., "2022 - 2026"
        String representativeName,
        String representativeEmail,
        boolean isActive
) {}
