package com.prevpaper.content.dto;

import java.io.Serializable;
import java.util.UUID;

public record ContentSearchResponseDTO(
        UUID id,
        String title,
        String description,
        String contentType,
        UUID universityId,
        UUID departmentId,
        UUID programId,
        Integer academicYear,
        Integer semester,
        UUID subjectId,
        UUID examTypeId,
        String fileUrl,
        String fileType
) implements Serializable {
    private static final long serialVersionUID = 1L;
}