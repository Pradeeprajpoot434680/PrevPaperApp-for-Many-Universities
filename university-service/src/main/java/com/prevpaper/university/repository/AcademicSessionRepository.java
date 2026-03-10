package com.prevpaper.university.repository;

import com.prevpaper.university.entities.AcademicSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AcademicSessionRepository extends JpaRepository<AcademicSession, UUID> {
}
