package com.prevpaper.notification.factory;



import com.prevpaper.notification.enums.NotificationType;
import com.prevpaper.notification.sender.NotificationSender;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NotificationFactory {

    private final List<NotificationSender> senders;

    public NotificationFactory(List<NotificationSender> senders) {
        this.senders = senders;
    }

    public NotificationSender getSender(NotificationType type) {

        return senders.stream()
                .filter(sender -> sender.supports(type))
                .findFirst()
                .orElseThrow(() ->
                        new RuntimeException("No sender found for type: " + type));
    }
}

/*
*
* NotificationSender sender =
        notificationFactory.getSender(NotificationType.EMAIL);

sender.sendNotification(userId, title, message);
*
* */