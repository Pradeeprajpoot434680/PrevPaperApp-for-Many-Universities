package com.prevpaper.notification.entities;

import com.prevpaper.comman.enums.NotificationEventType;
import com.prevpaper.comman.enums.NotificationType;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID userId;
    private String recipient;

    private String title;
    private String message;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    private List<NotificationType> notificationType; // ✅ from common-lib

    @Enumerated(EnumType.STRING)
    private NotificationEventType eventType;         // ✅ from common-lib

    private Boolean isRead = false;
    private LocalDateTime createdAt = LocalDateTime.now();
}