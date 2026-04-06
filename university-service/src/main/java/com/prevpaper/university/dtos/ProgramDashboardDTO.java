package com.prevpaper.university.dtos;

import java.util.UUID;

public record ProgramDashboardDTO(
        UUID id,
        String name,
        String code,
        String duration, // Hardcoded for now or add to Entity later
        String representativeName, // Null if not assigned
        long studentCount, // To show "Current Intake"
        Boolean active
) {}