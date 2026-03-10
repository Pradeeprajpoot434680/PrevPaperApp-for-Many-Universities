package com.prevpaper.notification.services.Impl;

import com.prevpaper.notification.dto.NotificationRequest;
import com.prevpaper.notification.entities.Notification;
import com.prevpaper.notification.enums.NotificationType;
import com.prevpaper.notification.factory.NotificationFactory;
import com.prevpaper.notification.repositories.NotificationRepository;
import com.prevpaper.notification.sender.NotificationSender;
import com.prevpaper.notification.services.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationFactory notificationFactory;
    private  final NotificationRepository notificationRepository;


    @Override
    public void sendNotification(NotificationRequest request) {
        Notification notification = new Notification();
        notification.setUserId(request.getUserId());
        notification.setTitle(request.getTitle());
        notification.setMessage(request.getMessage());
        notification.setEventType(request.getEventType());

        notificationRepository.save(notification);

        //send notification through selected channels

        for(NotificationType type: request.getNotificationTypes() ){
            NotificationSender sender = notificationFactory.getSender(type);
            sender.sendNotification(
                    request.getUserId(),
                    request.getRecipient(),
                    request.getTitle(),
                    request.getMessage()
            );
        }
    }

    @Override
    public void markAsRead(UUID notificationId) {

        Notification notification =
                notificationRepository.findById(notificationId)
                        .orElseThrow(() -> new RuntimeException("Notification not found"));

        notification.setIsRead(true);

        notificationRepository.save(notification);
    }
}
