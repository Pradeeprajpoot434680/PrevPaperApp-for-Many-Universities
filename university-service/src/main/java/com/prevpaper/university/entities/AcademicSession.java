package com.prevpaper.university.entities;



import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;
@Entity
@Table(name = "academic_sessions",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"program_id", "startYear"})})
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AcademicSession {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id")
    private Program program;

    private Integer startYear; // e.g., 2022
    private Integer endYear;   // e.g., 2026

    @Builder.Default
    private Boolean isActive = true;

    // Helper to return "Batch 2022"
    public String getName() {
        return "Batch " + startYear;
    }

    // Helper to return "2022 - 2026"
    public String getBatchRange() {
        return startYear + " - " + endYear;
    }
}