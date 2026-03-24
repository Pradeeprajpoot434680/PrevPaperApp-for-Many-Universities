package com.prevpaper.university.repository;

import com.prevpaper.university.entities.Program;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProgramRepository extends JpaRepository<Program, UUID> {
    boolean existsByCodeAndDepartmentId(String code, UUID departmentId);

    boolean existsByNameIgnoreCaseAndDepartmentId(String name, UUID departmentId);
}
