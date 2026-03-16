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
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "universities")
public class University {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String code; // e.g., CURAJ, DU, RTU

    @Column(unique = true, nullable = false)
    private String slug; // For clean URLs: /univ/curaj

    @Column(length = 1000)
    private String description;

    // Geographic Info
    private String country;
    private String state;
    private String city;

    // Brand & Web Info
    private String logoUrl;
    private String websiteUrl;
    private String emailDomain; // e.g., "curaj.ac.in" to auto-verify students

    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}