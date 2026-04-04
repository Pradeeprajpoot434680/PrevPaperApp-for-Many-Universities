package com.prevpaper.university.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "exam_configurations")
public class ExamConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // The name of the exam, e.g., "CIA-1", "Mid-Sem", "End-Sem"
    @Column(nullable = false)
    private String examName;

    // Optional: Add a display order so CIA-1 always shows before CIA-2
    private Integer displayOrder;

    @Builder.Default
    private Boolean isActive = true;

    // CONNECTION TO UNIVERSITY
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "university_id", nullable = false)
    private University university;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
