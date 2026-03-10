package com.prevpaper.university.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "semesters")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Semester {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id; //

    @Column(nullable = false)
    private Integer semesterNumber; // e.g., 1..8

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id", nullable = false)
    private Program program; //

    @OneToMany(mappedBy = "semester", cascade = CascadeType.ALL)
    private List<Subject> subjects;//
}