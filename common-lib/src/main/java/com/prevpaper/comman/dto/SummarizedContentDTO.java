package com.prevpaper.comman.dto;

import com.prevpaper.comman.enums.NotificationEventType;
import com.prevpaper.comman.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SummarizedContentDTO {
    private UUID contentId;
    private String title;
    private String fileUrl; // The link from Cloudinary
    private UUID universityId;
    private String status;

    private String recipient; // The user's Gmail address
    private UUID userId;      // For auditing/logging
    private NotificationEventType eventType; // UPLOAD_SUCCESS or UPLOAD_FAILURE
    private List<NotificationType> notificationTypes;
}
