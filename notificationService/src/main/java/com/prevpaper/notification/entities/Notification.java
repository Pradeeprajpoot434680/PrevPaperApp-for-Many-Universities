package com.prevpaper.notification.entities;

import com.prevpaper.notification.enums.NotificationEventType;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private UUID userId;
    private String title;
    private String message;
    @Enumerated(EnumType.STRING)
    private NotificationEventType eventType;
    private Boolean isRead = false;
    private LocalDateTime createdAt = LocalDateTime.now();
}
