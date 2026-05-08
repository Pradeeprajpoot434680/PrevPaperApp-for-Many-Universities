package com.prevpaper.comman.dto;



import com.prevpaper.comman.enums.ContentType;

import java.time.LocalDateTime;
import java.util.UUID;

public record PendingContentDTO(
        UUID contentId,
        String title,
        String description,
        String uploaderName,    // Fetch from User-Service using uploadedBy UUID
        String fileUrl,
        String fileType,
        LocalDateTime uploadedAt,
        String subjectName,
        ContentType contentType
) {
}
