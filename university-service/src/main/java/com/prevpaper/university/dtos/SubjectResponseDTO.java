package com.prevpaper.university.dtos;

import java.io.Serializable;
import java.util.UUID;

/**
 * 🟢 UNBREAKABLE DTO: Strips out lazy relations, entity structures, and raw
 * LocalDateTime properties to completely avoid Jackson serialization crashes.
 */
public record SubjectResponseDTO(
        UUID id,
        String name,
        String subjectCode,
        UUID semesterId
) implements Serializable {
    private static final long serialVersionUID = 1L;
}