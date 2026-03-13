package com.prevpaper.notification.services.impl;

import com.prevpaper.comman.dto.CommonNotificationRequest;
import com.prevpaper.comman.enums.NotificationType;
import com.prevpaper.notification.entities.Notification;
import com.prevpaper.notification.factory.NotificationFactory;
import com.prevpaper.notification.repositories.NotificationRepository;
import com.prevpaper.notification.sender.NotificationSender;
import com.prevpaper.notification.services.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationFactory notificationFactory;
    private final NotificationRepository notificationRepository;

    @Override
    @Transactional
    public void sendNotification(CommonNotificationRequest request) {
        Notification notification = new Notification();
        notification.setUserId(request.getUserId());
        notification.setRecipient(request.getRecipient());
        notification.setTitle(request.getTitle());
        notification.setMessage(request.getMessage());
        notification.setNotificationType(request.getNotificationTypes()); // now works
        notification.setEventType(request.getEventType());
        notification.setIsRead(false);
        notificationRepository.save(notification);

        if (request.getNotificationTypes() != null) {
            for (NotificationType type : request.getNotificationTypes()) {
                NotificationSender sender = notificationFactory.getSender(type);
                if (sender != null) {
                    sender.sendNotification(
                            request.getUserId(),
                            request.getRecipient(),
                            request.getTitle(),
                            request.getMessage()
                    );
                }
            }
        }
    }

    @Override
    @Transactional
    public void markAsRead(UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        notification.setIsRead(true);
        notificationRepository.save(notification);
    }
}