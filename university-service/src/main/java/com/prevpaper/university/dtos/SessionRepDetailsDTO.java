package com.prevpaper.university.dtos;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

public record SessionRepDetailsDTO(
        UUID assignmentId,
        UUID userId,
        String fullName,
        String email,
        UUID sessionId,
        String sessionName, // e.g., "Batch 2022"
        String batchRange,  // e.g., "2022 - 2026"
        @JsonSerialize(using = LocalDateTimeSerializer.class)
        @JsonDeserialize(using = LocalDateTimeDeserializer.class)
        LocalDateTime assignedAt,
        Boolean isActive
)implements Serializable {
    private static final long serialVersionUID = 1L;
}