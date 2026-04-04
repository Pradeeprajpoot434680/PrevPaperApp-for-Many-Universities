package com.prevpaper.user.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcademicUpdateRequest {

    @NotNull(message = "University ID is required")
    private UUID universityId;

    @NotNull(message = "Department ID is required")
    private UUID departmentId;

    @NotNull(message = "Program ID is required")
    private UUID programId;

    @NotNull(message = "Batch Year is required")
    private Integer batchYear; // e.g., 2024


    private Integer currentSemester;
}