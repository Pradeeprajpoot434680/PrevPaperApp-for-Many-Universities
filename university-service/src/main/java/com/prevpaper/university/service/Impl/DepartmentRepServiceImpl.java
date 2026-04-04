package com.prevpaper.university.service.Impl;

import com.prevpaper.comman.enums.ScopeType;
import com.prevpaper.comman.enums.UserRole;
import com.prevpaper.comman.exception.ResourceNotFoundException;
import com.prevpaper.university.dtos.AssignRepRequest;
import com.prevpaper.university.dtos.DepartmentTinyDTO;
import com.prevpaper.university.dtos.ProgramRequest;
import com.prevpaper.university.entities.Department;
import com.prevpaper.university.entities.Program;
import com.prevpaper.university.entities.RepresentativeAssignment;
import com.prevpaper.university.entities.University;
import com.prevpaper.university.repository.DepartmentRepository;
import com.prevpaper.university.repository.ProgramRepository;
import com.prevpaper.university.repository.RepresentativeRepository;
import com.prevpaper.university.repository.UniversityRepository;
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
    public DepartmentRepServiceImpl(ProgramRepository programRepository, RepresentativeRepository representativeRepository, EmitRoleAssignment emitRoleAssignment, DepartmentRepository departmentRepository, UniversityRepository universityRepository) {
        this.programRepository = programRepository;
        this.representativeRepository = representativeRepository;
        this.emitRoleAssignment = emitRoleAssignment;
        this.departmentRepository = departmentRepository;
        this.universityRepository = universityRepository;
    }

    @Override
    @Transactional
    public Program createProgram(UUID departmentId, ProgramRequest request) {

        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Program name is required");
        }

        if (request.getCode() == null || request.getCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Program code is required");
        }

        String name = request.getName().trim();
        String code = request.getCode().trim().toUpperCase();

        if (programRepository.existsByCodeAndDepartmentId(code, departmentId)) {
            throw new RuntimeException("Program with code " + code + " already exists");
        }

        if (programRepository.existsByNameIgnoreCaseAndDepartmentId(name, departmentId)) {
            throw new RuntimeException("Program with name " + name + " already exists");
        }

        Program program = Program.builder()
                .name(name)
                .code(code)
                .department(Department.builder().id(departmentId).build())
                .build();

        return programRepository.save(program);
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
