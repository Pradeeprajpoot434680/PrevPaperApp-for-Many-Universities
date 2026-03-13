package com.prevpaper.notification.entities;

import com.prevpaper.comman.enums.NotificationType;
import com.prevpaper.notification.enums.DeliveryStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notification_logs")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID notificationId; // Link back to the business notification

    @Enumerated(EnumType.STRING)
    private NotificationType providerType; // EMAIL, SMS, etc.

    @Enumerated(EnumType.STRING)
    private DeliveryStatus status; // SENT, FAILED, PENDING

    @Column(columnDefinition = "TEXT")
    private String providerResponse; // Store raw JSON error or Success ID

    private Integer retryCount = 0;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}

