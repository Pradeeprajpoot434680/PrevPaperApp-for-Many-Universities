package com.prevpaper.notification.controllers;

import com.prevpaper.notification.dto.NotificationRequest;
import com.prevpaper.notification.services.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/notification")
@RequiredArgsConstructor
public class NotificationController {
    private  final NotificationService notificationService;

    @PostMapping("/send")
    public String sendNotification(@RequestBody NotificationRequest request) {

        notificationService.sendNotification(request);

        return "Notification sent successfully";
    }

    // Mark notification as read
    @PutMapping("/read/{notificationId}")
    public String markAsRead(@PathVariable UUID notificationId) {

        notificationService.markAsRead(notificationId);

        return "Notification marked as read";
    }
}
