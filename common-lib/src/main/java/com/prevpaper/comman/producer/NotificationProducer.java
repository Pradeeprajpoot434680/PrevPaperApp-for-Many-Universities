package com.prevpaper.comman.producer;

import com.prevpaper.comman.dto.CommonNotificationRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public NotificationProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    private static final String HIGH_PRIORITY_TOPIC = "high-priority-notifications";
    private static final String BULK_TOPIC = "bulk-notifications";

    public void sendOtpNotification(String key, CommonNotificationRequest request) {
        System.out.println("Reached");
        kafkaTemplate.send(HIGH_PRIORITY_TOPIC, key, request);
    }

    public void sendBulkNotification(String key, CommonNotificationRequest request) {
        kafkaTemplate.send(BULK_TOPIC, key, request);
    }
}


