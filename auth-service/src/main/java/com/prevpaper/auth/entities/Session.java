package com.prevpaper.auth.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Session {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID sessionId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 512)
    private String refreshToken; // Hashed

    private String userAgent;

    @Pattern(
            regexp = "^(?:\\d{1,3}\\.){3}\\d{1,3}$",
            message = "Invalid IPv4 address"
    )
    private String ipAddress;

    private LocalDateTime expiresAt;

    private boolean isRevoked;
}