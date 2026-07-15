//package com.prevpaper.comman.config;
//
//import com.prevpaper.comman.dto.CommonNotificationRequest;
//import com.prevpaper.comman.producer.NotificationProducer;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.support.TransactionSynchronization;
//import org.springframework.transaction.support.TransactionSynchronizationManager;
//
//@Service
//@Slf4j
//public class SendNotification {
//    private final NotificationProducer producer;
//
//    public SendNotification(NotificationProducer producer) {
//        this.producer = producer;
//    }
//
//    public void sendOTP(CommonNotificationRequest request) {
//        // 🟢 FIXED: Check if an active database transaction actually exists
//        if (TransactionSynchronizationManager.isSynchronizationActive()) {
//            log.info("Active transaction detected. Registering Kafka notification hook for after-commit execution.");
//
//            TransactionSynchronizationManager.registerSynchronization(
//                    new TransactionSynchronization() {
//                        @Override
//                        public void afterCommit() {
//                            log.info("Database transaction committed successfully! Dispatched OTP to Kafka broker.");
//                            producer.sendOtpNotification(request.getUserId().toString(), request);
//                        }
//                    }
//            );
//        } else {
//            // 🟢 FALLBACK: If no transaction is active, send it immediately so the message isn't dropped into a vacuum
//            log.warn("No active transaction found for synchronization context. Dispatching OTP immediately.");
//            producer.sendOtpNotification(request.getUserId().toString(), request);
//        }
//    }
//}


package com.prevpaper.comman.config;

import com.prevpaper.comman.dto.CommonNotificationRequest;
import com.prevpaper.comman.producer.NotificationProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SendNotification {
    private final NotificationProducer producer;

    public SendNotification(NotificationProducer producer) {
        this.producer = producer;
    }

    public void sendOTP(CommonNotificationRequest request) {
        // 🟢 FIXED: Removed nested thread-local handlers. Focus entirely on direct delivery.
        log.info("Directly dispatching OTP payload event to Kafka producer layer for User: {}", request.getUserId());
        producer.sendOtpNotification(request.getUserId().toString(), request);
    }
}