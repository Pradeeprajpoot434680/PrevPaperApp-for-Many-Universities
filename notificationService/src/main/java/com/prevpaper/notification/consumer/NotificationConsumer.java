package com.prevpaper.notification.consumer;

import com.prevpaper.comman.dto.CommonNotificationRequest;
import com.prevpaper.notification.services.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = "high-priority-notifications", groupId = "notification-group")
    public void consumeHighPriority(@Payload CommonNotificationRequest request) {
        log.info("HIGH PRIORITY notification received: {}", request);
        notificationService.sendNotification(request);
    }

    @KafkaListener(topics = "bulk-notifications", groupId = "notification-group")
    public void consumeBulk(@Payload CommonNotificationRequest request) {
        log.info("BULK notification received: {}", request);
        notificationService.sendNotification(request);
    }
}