package com.prevpaper.notification.services;

import com.prevpaper.comman.dto.CommonNotificationRequest;
import java.util.UUID;

public interface NotificationService {

    void sendNotification(CommonNotificationRequest request);

    void markAsRead(UUID notificationId);
}