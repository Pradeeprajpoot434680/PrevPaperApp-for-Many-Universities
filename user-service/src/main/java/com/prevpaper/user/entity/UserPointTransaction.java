package com.prevpaper.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;


@Entity
@Table(name = "point_transactions")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UserPointTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account account;

    private Integer pointsChanged; // e.g., +10 or -50
    private String reason; // e.g., "PAPER_UPLOAD_BONUS"
    private String referenceId; // ID of the content uploaded

    @CreationTimestamp
    private LocalDateTime transactionDate;
}
