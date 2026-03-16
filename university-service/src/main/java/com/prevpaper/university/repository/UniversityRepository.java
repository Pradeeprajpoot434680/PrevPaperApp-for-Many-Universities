package com.prevpaper.university.repository;

import com.prevpaper.university.entities.University;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UniversityRepository extends JpaRepository<University, UUID> {
    boolean existsByName(String name);
    boolean existsByCode(String code);
}
