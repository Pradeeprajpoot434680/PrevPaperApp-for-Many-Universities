package com.prevpaper.notification.sender;

import com.prevpaper.comman.enums.NotificationType;
import com.twilio.Twilio;
import com.twilio.rest.verify.v2.service.Verification; // Imported specifically for Verify
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SmsNotificationSender implements NotificationSender {

    @Value("${twilio.account_sid}")
    private String accountSid;

    @Value("${twilio.auth_token}")
    private String authToken;

    @Value("${twilio.phone_number}")
    private String fromNumber;

    @Value("${twilio.verify_service_sid}")
    private String verifyServiceSid;

    @PostConstruct
    public void initTwilio() {
        Twilio.init(accountSid, authToken);
    }

    @Override
    public void sendNotification(UUID userId, String recipient, String title, String messageBody) {
        try {
            // 1. Phone Number Format Guard
            // Twilio Verify requires E.164 format (+91...)
            String formattedRecipient = recipient.trim();
            if (!formattedRecipient.startsWith("+")) {
                // If it's 10 digits without code, assume India (+91) as default for your project
                if (formattedRecipient.length() == 10) {
                    formattedRecipient = "+91" + formattedRecipient;
                } else {
                    System.err.println("Invalid Phone Format: " + recipient);
                    return;
                }
            }

            // 2. Trigger Twilio Verify
            Verification verification = Verification.creator(
                    verifyServiceSid,
                    formattedRecipient,
                    "sms"
            ).create();

            System.out.println("OTP Sent via Twilio Verify to " + formattedRecipient + ". SID: " + verification.getSid());

        } catch (Exception e) {
            // Log the error but don't re-throw if it's a permanent validation error
            // This prevents Kafka from retrying "bad" data forever
            System.err.println("Twilio API Error for user " + userId + ": " + e.getMessage());
        }
    }

    @Override
    public boolean supports(NotificationType type) {
        return type == NotificationType.SMS;
    }
}