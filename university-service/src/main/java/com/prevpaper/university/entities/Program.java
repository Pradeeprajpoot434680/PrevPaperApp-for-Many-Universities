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
    private UUID id; // [cite: 7]

    @Column(nullable = false)
    private String name; // e.g., B.Tech CSE [cite: 7]

    @Column(nullable = false)
    private  String code; // BTECH

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department; // Reference to Department [cite: 7]
}