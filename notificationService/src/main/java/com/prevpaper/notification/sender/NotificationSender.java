package com.prevpaper.notification.sender;


import com.prevpaper.notification.enums.NotificationType;

import java.util.UUID;

public interface NotificationSender {
    // Added recipient parameter
    void sendNotification(UUID userId, String recipient, String title, String message);

    boolean supports(NotificationType type);
}