package com.prevpaper.auth.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;


@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long logId;

    @ManyToOne
    @JoinColumn(name = "actor_id")
    private User actor;

    private String action;
    private String targetId;

    private String ipAddress;
    private String httpMethod;
    private String url;
    private String userAgent;

    @CreationTimestamp
    private LocalDateTime createdAt;
}