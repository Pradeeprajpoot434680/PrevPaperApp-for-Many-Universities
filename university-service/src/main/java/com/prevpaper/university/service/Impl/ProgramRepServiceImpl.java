package com.prevpaper.university.service.Impl;

import com.prevpaper.comman.enums.ScopeType;
import com.prevpaper.comman.enums.UserRole;
import com.prevpaper.university.dtos.AssignRepRequest;
import com.prevpaper.university.dtos.SessionRequest;
import com.prevpaper.university.entities.AcademicSession;
import com.prevpaper.university.entities.Program;
import com.prevpaper.university.entities.RepresentativeAssignment;
import com.prevpaper.university.repository.AcademicSessionRepository;
import com.prevpaper.university.repository.RepresentativeRepository;
import com.prevpaper.university.service.ProgramRepService;
import com.prevpaper.university.utils.EmitRoleAssignment;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProgramRepServiceImpl implements ProgramRepService {

    private final AcademicSessionRepository sessionRepository;
    private final RepresentativeRepository representativeRepository;
    private final EmitRoleAssignment emitRoleAssignment;

    @Override
    @Transactional
    public AcademicSession createSession(UUID programId,SessionRequest request) {

        boolean exists = sessionRepository.existsByProgramIdAndStartYearAndEndYear(
               programId,
                request.getStartYear(),
                request.getEndYear()
        );

        if (exists) {
            throw new RuntimeException("Session " + request.getStartYear() + "-" +
                    request.getEndYear() + " already exists for this program.");
        }

        AcademicSession session = AcademicSession.builder()
                .program(Program.builder().id(programId).build())
                .startYear(request.getStartYear())
                .endYear(request.getEndYear())
                .isActive(true)
                .build();

        return sessionRepository.save(session);
    }

    @Override
    @Transactional
    public void assignSessionRep(AssignRepRequest request, UUID adminId) {
        RepresentativeAssignment assignment = RepresentativeAssignment.builder()
                .userId(request.getUserId())
                .roles(Set.of(UserRole.SESSION_REP)) //
                .scopeType(ScopeType.SESSION) // [cite: 32]
                .scopeId(request.getScopeId()) // Session ID [cite: 33]
                .isActive(true) // [cite: 34]
                .assignedBy(adminId)
                .assignedAt(LocalDateTime.now())
                .build();
        representativeRepository.save(assignment);

        int programRoleId = 5;

        emitRoleAssignment.sendEmittedRoleAssignmentToKafka(request.getUserId(),programRoleId,request.getScopeId());

    }
}
