package com.prevpaper.university.repository;

import com.prevpaper.university.entities.AcademicSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AcademicSessionRepository extends JpaRepository<AcademicSession, UUID> {
    boolean existsByProgramIdAndStartYearAndEndYear(UUID programId, Integer startYear, Integer endingYear);
    List<AcademicSession> findByProgramId(UUID programId);
    List<AcademicSession> findByProgramIdIn(List<UUID> programIds);
}
