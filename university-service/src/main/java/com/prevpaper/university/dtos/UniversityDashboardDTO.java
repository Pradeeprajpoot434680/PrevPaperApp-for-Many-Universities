package com.prevpaper.university.dtos;

import java.util.UUID;

public record UniversityDashboardDTO(
        UUID id,
        String name,
        String location,
        String representativeName, // Null if not assigned
        String status // "Active" or "Pending" (if no rep)
) {}