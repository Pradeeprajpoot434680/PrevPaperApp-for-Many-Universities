package com.prevpaper.comman.producer;

import com.prevpaper.comman.dto.CommonNotificationRequest;
import com.prevpaper.comman.dto.SummarizedContentDTO;
import com.prevpaper.comman.enums.NotificationEventType;
import com.prevpaper.comman.enums.NotificationType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

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

    public void sendContentUploadNotification(SummarizedContentDTO summary, boolean isSuccess) {
        // 1. Generate the message based on success/failure
        String message = isSuccess
                ? "Your paper '" + summary.getTitle() + "' was uploaded successfully and is pending review."
                : "Upload failed for '" + summary.getTitle() + "'. Please try again.";

        // 2. Map the Summary info into the CommonNotificationRequest
        CommonNotificationRequest request = CommonNotificationRequest.builder()
                .userId(summary.getUserId())           // For tracking [cite: 30, 31]
                .recipient(summary.getRecipient())     // Crucial: The actual Gmail address
                .title(isSuccess ? "Upload Success" : "Upload Failed")
                .message(message)
                .eventType(isSuccess ? NotificationEventType.UPLOAD_SUCCESS : NotificationEventType.UPLOAD_FAILURE)
                .notificationTypes(List.of(NotificationType.EMAIL))
                .build();

        // 3. Send to Kafka using ContentId as the key for partition ordering
        // log.info("Emitting Upload Notification to Kafka for: {}", summary.getRecipient());// for logging use all the things
        kafkaTemplate.send(BULK_TOPIC, summary.getContentId().toString(), request);
    }
    public void sendBulkNotification(String key, CommonNotificationRequest request) {
        kafkaTemplate.send(BULK_TOPIC, key, request);
    }
}


