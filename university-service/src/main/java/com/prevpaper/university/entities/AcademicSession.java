package com.prevpaper.university.entities;



import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "academic_sessions",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"program_id", "startYear"})}) //
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AcademicSession {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id; // [cite: 9]

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id")
    private Program program; //

    private Integer startYear; //
    private Integer endYear; //
    private Boolean isActive; //
}