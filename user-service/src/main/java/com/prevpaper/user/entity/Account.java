package com.prevpaper.user.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.UUID;
import com.prevpaper.comman.enums.AccountStatus;

@Entity
@Table(name = "accounts")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    @NotNull
    private UUID universityId;
    @NotNull
    private UUID departmentId;
    @NotNull
    private UUID programId;
    @NotNull
    private Integer batchYear;

    private Long totalPoints = 0L;

    @Enumerated(EnumType.STRING)
    private AccountStatus status;
}