package com.prevpaper.university.dtos; // or your local DTO package

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class AssignRepRequest {
    private UUID userId;
    private UUID scopeId;

    // 🟢 FIXED: Teaches Spring Cache Eviction Evaluators how to parse this request object structure safely
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime expiresAt;
}