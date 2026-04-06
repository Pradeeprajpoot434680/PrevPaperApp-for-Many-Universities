package com.prevpaper.university.service.Impl;

import com.prevpaper.comman.dto.UserInternalInfoDTO;
import com.prevpaper.comman.enums.ScopeType;
import com.prevpaper.comman.enums.UserRole;
import com.prevpaper.university.client.AuthClient;
import com.prevpaper.university.client.UserServiceClient;
import com.prevpaper.university.dtos.AssignRepRequest;
import com.prevpaper.university.dtos.SessionDashboardDTO;
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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProgramRepServiceImpl implements ProgramRepService {

    private final AcademicSessionRepository sessionRepository;
    private final RepresentativeRepository representativeRepository;
    private final EmitRoleAssignment emitRoleAssignment;
    private final UserServiceClient userServiceClient;
    private final  AcademicSessionRepository academicSessionRepository;
    private final AuthClient authClient;

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


    @Override
    public List<SessionDashboardDTO> getProgramSessionsDashboard(UUID programId) {
        // 1. Fetch all sessions for this program
        List<AcademicSession> sessions = academicSessionRepository.findByProgramId(programId);

        return sessions.stream().map(session -> {
            // 2. Look for active Session Rep (CR) in the assignment table
            Optional<RepresentativeAssignment> assignment = representativeRepository
                    .findByScopeIdAndScopeTypeAndIsActiveTrue(session.getId(), ScopeType.SESSION);

            String repName = null;
            String repEmail = null;

            if (assignment.isPresent()) {
                UUID studentId = assignment.get().getUserId();
                try {
                    // Fetch Name from User-Service
                    repName = userServiceClient.getStudentName(studentId);
                    // Fetch Email from Auth-Service
                    UserInternalInfoDTO authInfo = authClient.getAuthUserInfo(studentId);
                    repEmail = (authInfo != null) ? authInfo.getEmail() : "N/A";
                } catch (Exception e) {
                    repName = "Profile Pending";
                    repEmail = "N/A";
                }
            }

            return new SessionDashboardDTO(
                    session.getId(),
                    session.getName(),       // Uses our helper: "Batch 2022"
                    session.getBatchRange(), // Uses our helper: "2022 - 2026"
                    repName,
                    repEmail,
                    session.getIsActive()
            );
        }).toList();
    }
}
