package com.prevpaper.notification.sender;

import com.prevpaper.comman.enums.NotificationType;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SmsNotificationSender implements NotificationSender {

    @Override
    public void sendNotification(UUID userId, String recipient, String title, String message) {
        System.out.println("Sending SMS to phone: " + recipient);
        System.out.println("Message: " + message);
    }

    @Override
    public boolean supports(NotificationType type) {
        return type == NotificationType.SMS;
    }
}
