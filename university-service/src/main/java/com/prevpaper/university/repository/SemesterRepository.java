package com.prevpaper.university.repository;

import com.prevpaper.university.entities.Semester;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SemesterRepository extends JpaRepository<Semester, UUID> {
    boolean existsBySemesterNumber(Integer semesterNumber);

    // Better version for your isolation logic:
    // This ensures semester 1 is unique ONLY within a specific program
    boolean existsBySemesterNumberAndProgramId(Integer semesterNumber, UUID programId);
}
