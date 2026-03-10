package com.prevpaper.university.service.Impl;

import com.prevpaper.comman.enums.ScopeType;
import com.prevpaper.comman.enums.UserRole;
import com.prevpaper.university.dtos.AssignRepRequest;
import com.prevpaper.university.dtos.ProgramRequest;
import com.prevpaper.university.entities.Department;
import com.prevpaper.university.entities.Program;
import com.prevpaper.university.entities.RepresentativeAssignment;
import com.prevpaper.university.repository.ProgramRepository;
import com.prevpaper.university.repository.RepresentativeRepository;
import com.prevpaper.university.service.DepartmentRepService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;


@Service

public class DepartmentRepServiceImpl implements DepartmentRepService {
    private final ProgramRepository programRepository;
    private final RepresentativeRepository representativeRepository;

    public DepartmentRepServiceImpl(ProgramRepository programRepository, RepresentativeRepository representativeRepository) {
        this.programRepository = programRepository;
        this.representativeRepository = representativeRepository;
    }

    @Override
    @Transactional
    public Program createProgram(Program program) {

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
    }
};
