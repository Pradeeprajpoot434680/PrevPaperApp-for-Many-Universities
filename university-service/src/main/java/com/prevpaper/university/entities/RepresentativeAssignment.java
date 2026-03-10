package com.prevpaper.university.entities;


import com.prevpaper.comman.enums.ScopeType;
import com.prevpaper.comman.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "representative_assignments")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RepresentativeAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // ID of the user being assigned
    @Column(nullable = false)
    private UUID userId;

    // A user can have multiple roles within a scope
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "assignment_roles",
            joinColumns = @JoinColumn(name = "assignment_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "role_name", nullable = false)
    private Set<UserRole> roles;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScopeType scopeType;

    // The ID of University / Department / Program / Session
    @Column(nullable = false)
    private UUID scopeId;

    @Column(nullable = false)
    private Boolean isActive;

    // Admin who assigned this role
    @Column(nullable = false)
    private UUID assignedBy;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime assignedAt;

    private LocalDateTime expiresAt;
}