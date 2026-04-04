package com.prevpaper.university.repository;

import com.prevpaper.university.entities.ExamConfiguration;
import com.prevpaper.university.entities.University;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ExamConfigRepository extends JpaRepository<ExamConfiguration,UUID> {
    List<ExamConfiguration> findByUniversity(University university);

    // Optional: useful for the frontend to get sorted lists
    List<ExamConfiguration> findByUniversityOrderByDisplayOrderAsc(University university);
}
