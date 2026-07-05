package com.prevpaper.university.dtos;

import java.io.Serializable;
import java.util.UUID;

public record ProgramResponseDTO(
        UUID id,
        String name,
        String code,
        Integer durationYears,
        Integer totalSemesters,
        String description,
        Boolean isActive,
        UUID departmentId
) implements Serializable {
    private static final long serialVersionUID = 1L;
}