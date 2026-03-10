package com.prevpaper.notification.dto;

import com.prevpaper.notification.enums.NotificationEventType;
import com.prevpaper.notification.enums.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class NotificationRequest {

    @NotNull
    private UUID userId;

    @NotBlank
    private String title;

    @NotBlank
    private  String recipient;

    @NotBlank
    private String message;

    @NotNull
    private NotificationEventType eventType;

    // Which channels to send notification
    private List<NotificationType> notificationTypes;
}
