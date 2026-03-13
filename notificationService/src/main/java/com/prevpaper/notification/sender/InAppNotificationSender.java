package com.prevpaper.notification.sender;


import com.prevpaper.comman.enums.NotificationType;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class InAppNotificationSender implements NotificationSender {

    @Override
    public void sendNotification(UUID userId, String recipient, String title, String message) {
        System.out.println("Pushing In-App notification to User UUID: " + userId);
    }
    @Override
    public boolean supports(NotificationType type) {
        return type == NotificationType.IN_APP;
    }
}