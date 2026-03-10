package com.prevpaper.notification.services;

import com.prevpaper.notification.dto.NotificationRequest;

import java.util.UUID;

public interface NotificationService {


    void sendNotification(NotificationRequest request);

    void markAsRead(UUID notificationId);
}
