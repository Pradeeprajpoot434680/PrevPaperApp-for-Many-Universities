package com.prevpaper.notification.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class NotificationResponse {

    private UUID id;
    private UUID userId;
    private String title;
    private String message;
    private Boolean isRead;
    private LocalDateTime createdAt;

}
