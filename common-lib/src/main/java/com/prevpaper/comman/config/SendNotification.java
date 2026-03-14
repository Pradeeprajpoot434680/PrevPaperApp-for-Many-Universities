package com.prevpaper.comman.config;

import com.prevpaper.comman.dto.CommonNotificationRequest;
import com.prevpaper.comman.producer.NotificationProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;


@Service
public class SendNotification {
    private final NotificationProducer producer;

    public SendNotification(NotificationProducer producer) {
        this.producer = producer;
    }

    public   void sendOTP(CommonNotificationRequest request){
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        producer.sendOtpNotification(request.getUserId().toString(), request);
                    }
                }
        );
    }

}
