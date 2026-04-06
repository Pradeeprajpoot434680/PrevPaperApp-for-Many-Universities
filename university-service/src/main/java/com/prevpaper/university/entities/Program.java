package com.prevpaper.university.entities;



import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "programs", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"department_id", "code"}),
        @UniqueConstraint(columnNames = {"department_id", "name"})
})
public class Program {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name; // e.g., "Bachelor of Technology in Computer Science"

    @Column(nullable = false)
    private String code; // e.g., "BTECH-CS"

    @Column(nullable = false)
    private Integer durationYears; // e.g., 4

    @Column(nullable = false)
    private Integer totalSemesters; // e.g., 8

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}