package com.prevpaper.university.service.Impl;

import com.prevpaper.comman.dto.StudentDTO;
import com.prevpaper.comman.dto.UserBatchRequest;
import com.prevpaper.comman.dto.UserDetailDTO;
import com.prevpaper.comman.dto.UserInternalInfoDTO;
import com.prevpaper.comman.enums.ScopeType;
import com.prevpaper.comman.enums.UserRole;
import com.prevpaper.university.client.AuthClient;
import com.prevpaper.university.client.UserServiceClient;
import com.prevpaper.university.dtos.AssignRepRequest;
import com.prevpaper.university.dtos.SessionDashboardDTO;
import com.prevpaper.university.dtos.SessionRepDetailsDTO;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
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
        log.info("Create academic session request received: programId={}, startYear={}, endYear={}",
                programId, request.getStartYear(), request.getEndYear());

        boolean exists = sessionRepository.existsByProgramIdAndStartYearAndEndYear(
               programId,
                request.getStartYear(),
                request.getEndYear()
        );

        if (exists) {
            log.warn("Create academic session rejected: duplicate session, programId={}, startYear={}, endYear={}",
                    programId, request.getStartYear(), request.getEndYear());
            throw new RuntimeException("Session " + request.getStartYear() + "-" +
                    request.getEndYear() + " already exists for this program.");
        }

        AcademicSession session = AcademicSession.builder()
                .program(Program.builder().id(programId).build())
                .startYear(request.getStartYear())
                .endYear(request.getEndYear())
                .isActive(true)
                .build();

        AcademicSession savedSession = sessionRepository.save(session);
        log.info("Academic session created: sessionId={}, programId={}, startYear={}, endYear={}, active={}",
                savedSession.getId(), programId, savedSession.getStartYear(), savedSession.getEndYear(), savedSession.getIsActive());
        return savedSession;
    }

    @Override
    @Transactional
    public void assignSessionRep(AssignRepRequest request, UUID adminId) {
        log.info("Assign session representative request received: userId={}, scopeId={}, adminId={}",
                request.getUserId(), request.getScopeId(), adminId);
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
        log.info("Session representative assignment saved: assignmentId={}, userId={}, scopeId={}, adminId={}",
                assignment.getId(), request.getUserId(), request.getScopeId(), adminId);

        int programRoleId = 5;

        emitRoleAssignment.sendEmittedRoleAssignmentToKafka(request.getUserId(),programRoleId,request.getScopeId());
        log.info("Session representative role emit registered: userId={}, roleId={}, scopeId={}",
                request.getUserId(), programRoleId, request.getScopeId());

    }


    @Override
    public List<SessionDashboardDTO> getProgramSessionsDashboard(UUID programId) {
        log.info("Program sessions dashboard request received: programId={}", programId);
        // 1. Fetch all sessions for this program
        List<AcademicSession> sessions = academicSessionRepository.findByProgramId(programId);
        log.info("Program sessions loaded for dashboard: programId={}, sessionCount={}", programId, sessions.size());

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
                    log.warn("Session dashboard representative lookup failed: programId={}, sessionId={}, repUserId={}, error={}",
                            programId, session.getId(), studentId, e.getMessage());
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

    @Override
    public List<SessionRepDetailsDTO> getAllSessionRepsByProgram(UUID programId) {
        log.info("All session representatives by program request received: programId={}", programId);
        // 1. Get all sessions for this program
        List<AcademicSession> sessions = academicSessionRepository.findByProgramId(programId);
        List<UUID> sessionIds = sessions.stream().map(AcademicSession::getId).toList();
        log.info("Sessions loaded for representative lookup: programId={}, sessionCount={}",
                programId, sessions.size());

        if (sessionIds.isEmpty()) {
            log.info("No sessions found for representative lookup: programId={}", programId);
            return Collections.emptyList();
        }

        // 2. Find all active assignments for these SESSION scopes
        List<RepresentativeAssignment> assignments = representativeRepository
                .findByScopeIdInAndScopeTypeAndIsActiveTrue(sessionIds, ScopeType.SESSION);
        log.info("Session representative assignments loaded: programId={}, assignmentCount={}",
                programId, assignments.size());

        // 3. Collect User IDs for bulk enrichment
        List<UUID> userIds = assignments.stream()
                .map(RepresentativeAssignment::getUserId)
                .distinct()
                .toList();

        // 4. Batch Fetch Names (User-Service) and Emails (Auth-Service)
        Map<UUID, StudentDTO> profileMap = userServiceClient.getBulkUserDetails(userIds);
        log.info("Session representative profile details loaded: requestedUsers={}, receivedProfiles={}",
                userIds.size(), profileMap.size());

        UserBatchRequest batchRequest = new UserBatchRequest(userIds);
        Map<UUID, UserDetailDTO> authMap = authClient.getUserDetailsBatch(batchRequest)
                .stream().collect(Collectors.toMap(UserDetailDTO::userId, d -> d));
        log.info("Session representative auth details loaded: requestedUsers={}, receivedAuthDetails={}",
                userIds.size(), authMap.size());

        // 5. Map Sessions for O(1) lookup
        Map<UUID, AcademicSession> sessionMap = sessions.stream()
                .collect(Collectors.toMap(AcademicSession::getId, s -> s));

        // 6. Combine everything into the DTO
        return assignments.stream().map(rep -> {
            AcademicSession sess = sessionMap.get(rep.getScopeId());
            StudentDTO profile = profileMap.get(rep.getUserId());
            UserDetailDTO auth = authMap.get(rep.getUserId());

            return new SessionRepDetailsDTO(
                    rep.getId(),
                    rep.getUserId(),
                    profile != null ? profile.fullName() : "No Profile",
                    auth != null ? auth.email() : "No Email",
                    sess.getId(),
                    sess.getName(),
                    sess.getBatchRange(),
                    rep.getAssignedAt(),
                    rep.getIsActive()
            );
        }).toList();
    }
}
