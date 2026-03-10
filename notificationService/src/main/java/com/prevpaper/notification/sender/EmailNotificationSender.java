package com.prevpaper.notification.sender;

import com.prevpaper.notification.enums.NotificationType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import org.springframework.http.HttpHeaders;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class EmailNotificationSender implements NotificationSender {

    @Value("${resend.api.key}")
    private String apiKey;

    @Value("${resend.email.from}")
    private String fromEmail;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public void sendNotification(UUID userId, String recipientEmail, String title, String message) {
        String url = "https://api.resend.com/emails";
        Map<String, Object> body = new HashMap<>();

        // Dynamic 'to' field using the parameter
        body.put("from", "PrevYearPaper App <" + fromEmail + ">");
        body.put("to", List.of(recipientEmail)); // Use the real email here
        body.put("subject", title);

        body.put("html", """
            <div style="font-family:sans-serif;line-height:1.5;max-width:500px;margin:auto;border:1px solid #eee;padding:20px;border-radius:10px;">
              <h2 style="text-align:center;color:#333;">Verification Code</h2>
              <p>Hello,</p>
              <p>Your One-Time Password (OTP) for <strong>PrevYearPaper</strong> is:</p>
              <div style="text-align:center;margin:30px 0;">
                <span style="font-size:32px;font-weight:bold;color:#2E86DE;letter-spacing:5px;background:#f4f7f6;padding:10px 20px;border-radius:5px;">%s</span>
              </div>
              <p style="font-size:12px;color:#777;">This code will expire in 10 minutes. If you did not request this, please ignore this email.</p>
              <hr style="border:none;border-top:1px solid #eee;margin:20px 0;"/>
              <p style="font-size:12px;color:#999;text-align:center;">Sent by the PrevYearPaper Team</p>
            </div>
            """.formatted(message));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(body, headers);


        ResponseEntity<String> response =
                restTemplate.postForEntity(url, request, String.class);

        System.out.println("Email sent response: " + response.getBody());


    }

    @Override
    public boolean supports(NotificationType type) {
        return type == NotificationType.EMAIL;
    }
}
