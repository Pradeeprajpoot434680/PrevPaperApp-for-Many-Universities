// package com.prevpaper.comman.producer;

// import com.prevpaper.comman.dto.CommonNotificationRequest;
// import com.prevpaper.comman.dto.SummarizedContentDTO;
// import com.prevpaper.comman.enums.NotificationEventType;
// import com.prevpaper.comman.enums.NotificationType;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.kafka.core.KafkaTemplate;
// import org.springframework.stereotype.Service;

// import java.util.List;

// @Service
// @Slf4j
// public class NotificationProducer {

//     private final KafkaTemplate<String, Object> kafkaTemplate;

//     public NotificationProducer(KafkaTemplate<String, Object> kafkaTemplate) {
//         this.kafkaTemplate = kafkaTemplate;
//     }

//     private static final String HIGH_PRIORITY_TOPIC = "high-priority-notifications";
//     private static final String BULK_TOPIC = "bulk-notifications";

//     public void sendOtpNotification(String key, CommonNotificationRequest request) {
//         System.out.println("Reached");
//         kafkaTemplate.send(HIGH_PRIORITY_TOPIC, key, request);
//     }

//     public void sendContentUploadNotification(SummarizedContentDTO summary, boolean isSuccess) {
//         // 1. Generate the message based on success/failure
//         String message = isSuccess
//                 ? "Your paper '" + summary.getTitle() + "' was uploaded successfully and is pending review."
//                 : "Upload failed for '" + summary.getTitle() + "'. Please try again.";

//         // 2. Map the Summary info into the CommonNotificationRequest
//         CommonNotificationRequest request = CommonNotificationRequest.builder()
//                 .userId(summary.getUserId())           // For tracking [cite: 30, 31]
//                 .recipient(summary.getRecipient())     // Crucial: The actual Gmail address
//                 .title(isSuccess ? "Upload Success" : "Upload Failed")
//                 .message(message)
//                 .eventType(isSuccess ? NotificationEventType.UPLOAD_SUCCESS : NotificationEventType.UPLOAD_FAILURE)
//                 .notificationTypes(List.of(NotificationType.EMAIL))
//                 .build();

//         // 3. Send to Kafka using ContentId as the key for partition ordering
//         // log.info("Emitting Upload Notification to Kafka for: {}", summary.getRecipient());// for logging use all the things
//         kafkaTemplate.send(BULK_TOPIC, summary.getContentId().toString(), request);
//     }
//     public void sendBulkNotification(String key, CommonNotificationRequest request) {
//         kafkaTemplate.send(BULK_TOPIC, key, request);
//     }


//     // Add this method to your existing NotificationProducer class
//     public void sendContentStatusUpdateNotification(SummarizedContentDTO summary) {
//         boolean isVerified = "VERIFIED".equalsIgnoreCase(summary.getStatus());

//         // 1. Generate status-specific message
//         String message = isVerified
//                 ? "Great news! Your content '" + summary.getTitle() + "' has been verified and is now live in the library."
//                 : "Your content '" + summary.getTitle() + "' was not approved for the library. Please ensure it meets our guidelines.";

//         // 2. Map into the common request format
//         CommonNotificationRequest request = CommonNotificationRequest.builder()
//                 .userId(summary.getUserId())
//                 .recipient(summary.getRecipient()) // Real email fetched via Feign in Service layer
//                 .title(isVerified ? "Content Verified" : "Content Rejected")
//                 .message(message)
//                 .eventType(summary.getEventType()) // CONTENT_VERIFIED or CONTENT_REJECTED
//                 .notificationTypes(List.of(NotificationType.EMAIL))
//                 .build();

//         try {
//             kafkaTemplate.send(BULK_TOPIC, summary.getContentId().toString(), request);
//             log.info("KAFKA EVENT EMITTED SUCCESSFULLY to topic: {}", BULK_TOPIC);
//         } catch (Exception e) {
//             log.error("KAFKA SEND CRASHED: {}", e.getMessage());
//         }
//     }
// }


package com.prevpaper.comman.producer;

import com.prevpaper.comman.dto.CommonNotificationRequest;
import com.prevpaper.comman.dto.SummarizedContentDTO;
import com.prevpaper.comman.enums.NotificationEventType;
import com.prevpaper.comman.enums.NotificationType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class NotificationProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public NotificationProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    private static final String HIGH_PRIORITY_TOPIC = "high-priority-notifications";
    private static final String BULK_TOPIC = "bulk-notifications";

    public void sendOtpNotification(String key, CommonNotificationRequest request) {
        log.info("Firing high-priority OTP event notification payload to Kafka for key: {}", key);
        
        // 🟢 FIXED: Handle the returned complete future result thread to monitor failure logs
        kafkaTemplate.send(HIGH_PRIORITY_TOPIC, key, request)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("❌ CRITICAL: Failed to write OTP event onto high-priority topic due to exception!", ex);
                } else {
                    log.info("✓ SUCCESS: OTP verified on topic channel. Partition={}, Offset={}", 
                            result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                }
            });
    }

    public void sendContentUploadNotification(SummarizedContentDTO summary, boolean isSuccess) {
        String message = isSuccess
                ? "Your paper '" + summary.getTitle() + "' was uploaded successfully and is pending review."
                : "Upload failed for '" + summary.getTitle() + "'. Please try again.";

        CommonNotificationRequest request = CommonNotificationRequest.builder()
                .userId(summary.getUserId())           
                .recipient(summary.getRecipient())     
                .title(isSuccess ? "Upload Success" : "Upload Failed")
                .message(message)
                .eventType(isSuccess ? NotificationEventType.UPLOAD_SUCCESS : NotificationEventType.UPLOAD_FAILURE)
                .notificationTypes(List.of(NotificationType.EMAIL))
                .build();

        log.info("Emitting bulk paper upload event state to topic: {}", BULK_TOPIC);
        kafkaTemplate.send(BULK_TOPIC, summary.getContentId().toString(), request)
            .whenComplete((result, ex) -> {
                if (ex != null) log.error("❌ Bulk broadcast failed for upload state!", ex);
            });
    }

    public void sendBulkNotification(String key, CommonNotificationRequest request) {
        kafkaTemplate.send(BULK_TOPIC, key, request)
            .whenComplete((result, ex) -> {
                if (ex != null) log.error("❌ Bulk payload submission crashed!", ex);
            });
    }

    public void sendContentStatusUpdateNotification(SummarizedContentDTO summary) {
        boolean isVerified = "VERIFIED".equalsIgnoreCase(summary.getStatus());

        String message = isVerified
                ? "Great news! Your content '" + summary.getTitle() + "' has been verified and is now live in the library."
                : "Your content '" + summary.getTitle() + "' was not approved for the library. Please ensure it meets our guidelines.";

        CommonNotificationRequest request = CommonNotificationRequest.builder()
                .userId(summary.getUserId())
                .recipient(summary.getRecipient()) 
                .title(isVerified ? "Content Verified" : "Content Rejected")
                .message(message)
                .eventType(summary.getEventType()) 
                .notificationTypes(List.of(NotificationType.EMAIL))
                .build();

        kafkaTemplate.send(BULK_TOPIC, summary.getContentId().toString(), request)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("❌ KAFKA SEND CRASHED during status update verification: {}", ex.getMessage());
                } else {
                    log.info("✓ KAFKA EVENT EMITTED SUCCESSFULLY to topic: {}", BULK_TOPIC);
                }
            });
    }
}