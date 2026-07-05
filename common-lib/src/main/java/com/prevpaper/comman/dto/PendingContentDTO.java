package com.prevpaper.comman.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.prevpaper.comman.enums.ContentType;

import java.time.LocalDateTime;
import java.util.UUID;

public record PendingContentDTO(
        UUID contentId,
        String title,
        String description,
        String uploaderName,
        String fileUrl,
        String fileType,

        // 🟢 FIXED: Teaches Jackson Feign Decoders how to parse this field across microservices
        @JsonSerialize(using = LocalDateTimeSerializer.class)
        @JsonDeserialize(using = LocalDateTimeDeserializer.class)
        LocalDateTime uploadedAt,

        String subjectName,
        ContentType contentType
) {
}