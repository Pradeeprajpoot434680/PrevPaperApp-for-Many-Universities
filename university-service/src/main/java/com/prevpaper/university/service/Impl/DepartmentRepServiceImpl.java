package com.prevpaper.university.service.Impl;

import com.prevpaper.comman.enums.ScopeType;
import com.prevpaper.comman.enums.UserRole;
import com.prevpaper.comman.exception.ResourceNotFoundException;
import com.prevpaper.university.dtos.AssignRepRequest;
import com.prevpaper.university.dtos.DepartmentTinyDTO;
import com.prevpaper.university.dtos.ProgramRequest;
import com.prevpaper.university.entities.*;
import com.prevpaper.university.repository.*;
import com.prevpaper.university.service.DepartmentRepService;
import com.prevpaper.university.utils.EmitRoleAssignment;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict; // 🟢 IMPORTED
import org.springframework.cache.annotation.Cacheable; // 🟢 IMPORTED
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class DepartmentRepServiceImpl implements DepartmentRepService {
    private final ProgramRepository programRepository;
    private final RepresentativeRepository representativeRepository;
    private final EmitRoleAssignment emitRoleAssignment;
    private final DepartmentRepository departmentRepository;
    private final UniversityRepository universityRepository;
    private final SemesterRepository semesterRepository;

    public DepartmentRepServiceImpl(ProgramRepository programRepository,
                                    RepresentativeRepository representativeRepository,
                                    EmitRoleAssignment emitRoleAssignment,
                                    DepartmentRepository departmentRepository,
                                    UniversityRepository universityRepository,
                                    SemesterRepository semesterRepository) {
        this.programRepository = programRepository;
        this.representativeRepository = representativeRepository;
        this.emitRoleAssignment = emitRoleAssignment;
        this.departmentRepository = departmentRepository;
        this.universityRepository = universityRepository;
        this.semesterRepository = semesterRepository;
    }

    /**
     * MUTATION: Evicts the cached program lists for this specific department
     * so that the newly created program appears instantly on the UI.
     */
    @Override
    @Transactional
    @CacheEvict(value = "programs", key = "#departmentId") // 🟢 EVICTS STALE PROGRAMS LIST
    public Program createProgram(UUID departmentId, ProgramRequest request) {
        log.info("Redis Cache EVICT [programs] - Creating program for departmentId={}", departmentId);

        validateProgramRequest(request);

        String name = request.getName().trim();
        String code = request.getCode().trim().toUpperCase();

        if (programRepository.existsByCodeAndDepartmentId(code, departmentId)) {
            throw new RuntimeException("Program code " + code + " already exists");
        }

        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));

        Program program = Program.builder()
                .name(name)
                .code(code)
                .durationYears(request.getDurationYears())
                .totalSemesters(request.getTotalSemesters())
                .description(request.getDescription())
                .isActive(true)
                .department(department)
                .build();

        Program savedProgram = programRepository.save(program);

        // Automatically initialize semesters
        for (int i = 1; i <= savedProgram.getTotalSemesters(); i++) {
            Semester semester = Semester.builder()
                    .semesterNumber(i)
                    .program(savedProgram)
                    .build();
            semesterRepository.save(semester);
        }

        return savedProgram;
    }

    private void validateProgramRequest(ProgramRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Program name is required");
        }
        if (request.getCode() == null || request.getCode().isBlank()) {
            throw new IllegalArgumentException("Program code is required");
        }
        if (request.getDurationYears() == null || request.getDurationYears() <= 0) {
            throw new IllegalArgumentException("Valid duration in years is required");
        }
    }

    @Override
    @Transactional
    public void assignProgramRep(AssignRepRequest request, UUID adminId) {
        log.info("Assign program representative request received: userId={}, scopeId={}", request.getUserId(), request.getScopeId());
        RepresentativeAssignment assignment = RepresentativeAssignment.builder()
                .userId(request.getUserId())
                .roles(Set.of(UserRole.PROGRAM_REP))
                .scopeType(ScopeType.PROGRAM)
                .scopeId(request.getScopeId())
                .isActive(true)
                .assignedBy(adminId)
                .assignedAt(LocalDateTime.now())
                .build();
        representativeRepository.save(assignment);

        int DepartmentRoleId = 4;
        emitRoleAssignment.sendEmittedRoleAssignmentToKafka(request.getUserId(), DepartmentRoleId, request.getScopeId());
    }

    /**
     * READ CACHE: Caches the list of tiny department metadata objects by universityId.
     * Sharing this cache namespace with UniversityRepresentativeServiceImpl ensures data consistency.
     */
    @Override
    @Cacheable(value = "departments", key = "#universityId") // 🟢 READ CACHE FOR TINY DEPT DTOs
    public List<DepartmentTinyDTO> findDepartmentsByUniversityId(UUID universityId) {
        log.info("Redis Cache MISS - Loading tiny department DTO summaries from DB for universityId={}", universityId);

        if (!universityRepository.existsById(universityId)) {
            throw new ResourceNotFoundException("University not found");
        }

        List<Department> departments = departmentRepository.findByUniversityId(universityId);

        return departments.stream()
                .map(dept -> new DepartmentTinyDTO(
                        dept.getId(),
                        dept.getName(),
                        dept.getCode()
                ))
                .toList();
    }
}