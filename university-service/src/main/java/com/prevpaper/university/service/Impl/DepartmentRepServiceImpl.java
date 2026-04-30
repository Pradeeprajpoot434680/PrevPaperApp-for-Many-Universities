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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;


@Service

public class DepartmentRepServiceImpl implements DepartmentRepService {
    private final ProgramRepository programRepository;
    private final RepresentativeRepository representativeRepository;
    private  final EmitRoleAssignment emitRoleAssignment;
    private final DepartmentRepository departmentRepository;
    private final UniversityRepository universityRepository;
    private final SemesterRepository semesterRepository;



    public DepartmentRepServiceImpl(ProgramRepository programRepository, RepresentativeRepository representativeRepository, EmitRoleAssignment emitRoleAssignment, DepartmentRepository departmentRepository, UniversityRepository universityRepository, SemesterRepository semesterRepository) {
        this.programRepository = programRepository;
        this.representativeRepository = representativeRepository;
        this.emitRoleAssignment = emitRoleAssignment;
        this.departmentRepository = departmentRepository;
        this.universityRepository = universityRepository;
        this.semesterRepository = semesterRepository;
    }

    @Override
    @Transactional
    public Program createProgram(UUID departmentId, ProgramRequest request) {
        // 1. Basic Validations
        validateProgramRequest(request);

        String name = request.getName().trim();
        String code = request.getCode().trim().toUpperCase();

        // 2. Duplicate Checks (Existing logic)
        if (programRepository.existsByCodeAndDepartmentId(code, departmentId)) {
            throw new RuntimeException("Program code " + code + " already exists");
        }

        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));

        // 3. Save the Program first
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

        // 4. AUTOMATICALLY CREATE SEMESTERS
        // If totalSemesters is 8, this loops 1 to 8
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
        if (request.getName() == null || request.getName().isBlank())
            throw new IllegalArgumentException("Program name is required");
        if (request.getCode() == null || request.getCode().isBlank())
            throw new IllegalArgumentException("Program code is required");
        if (request.getDurationYears() == null || request.getDurationYears() <= 0)
            throw new IllegalArgumentException("Valid duration in years is required");
    }
    @Override
    @Transactional
    public void assignProgramRep(AssignRepRequest request, UUID adminId) {
        RepresentativeAssignment assignment = RepresentativeAssignment.builder()
                .userId(request.getUserId())
                .roles(Set.of(UserRole.PROGRAM_REP)) //
                .scopeType(ScopeType.PROGRAM) // [cite: 32]
                .scopeId(request.getScopeId()) // Program ID [cite: 33]
                .isActive(true) // [cite: 34]
                .assignedBy(adminId) // [cite: 34]
                .assignedAt(LocalDateTime.now())
                .build();
        representativeRepository.save(assignment);

        int DepartmentRoleId = 4;
        emitRoleAssignment.sendEmittedRoleAssignmentToKafka(request.getUserId(),DepartmentRoleId,request.getScopeId());


    }

    @Override
    public List<DepartmentTinyDTO> findDepartmentsByUniversityId(UUID universityId) {
        // 1. Verify university exists
        if (!universityRepository.existsById(universityId)) {
            throw new ResourceNotFoundException("University not found");
        }

        // 2. Fetch entities
        List<Department> departments = departmentRepository.findByUniversityId(universityId);

        // 3. Map to DTO
        return departments.stream()
                .map(dept -> new DepartmentTinyDTO(
                        dept.getId(),
                        dept.getName(),
                        dept.getCode()
                ))
                .toList();
    }
};
