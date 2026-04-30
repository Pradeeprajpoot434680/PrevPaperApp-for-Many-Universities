package com.prevpaper.university.repository;

import com.prevpaper.university.entities.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SubjectRepository extends JpaRepository<Subject, UUID> {
    boolean existsBySubjectCode(String subjectCode);

    // Check uniqueness of name within one specific semester
    boolean existsByNameIgnoreCaseAndSemesterId(String name, UUID semesterId);
    List<Subject> findBySemesterId(UUID semesterId);

    long countBySemesterId(UUID semesterId);
}
