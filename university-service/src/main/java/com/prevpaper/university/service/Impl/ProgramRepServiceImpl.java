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
import org.springframework.cache.annotation.CacheEvict; // 🟢 IMPORTED
import org.springframework.cache.annotation.Cacheable; // 🟢 IMPORTED
import org.springframework.cache.annotation.Caching;   // 🟢 IMPORTED
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
    private final AcademicSessionRepository academicSessionRepository;
    private final AuthClient authClient;

    /**
     * MUTATION: Clears the program dashboard caches and structural program configurations
     * immediately so that the newly created academic session displays on the frontend.
     */
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "programDashboards", key = "#programId"),
            @CacheEvict(value = "programStructure", key = "#programId")
    }) // 🟢 MULTI-CACHE EVICTION STRATEGY
    public AcademicSession createSession(UUID programId, SessionRequest request) {
        log.info("Redis Cache EVICT [programDashboards, programStructure] - Creating session for programId={}", programId);

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

    /**
     * MUTATION: Evicts active dashboard listings when a representative role changes.
     */
    @Override
    @Transactional
    @CacheEvict(value = "programDashboards", key = "#request.scopeId") // 🟢 PURGES OLD STATUS DATA
    public void assignSessionRep(AssignRepRequest request, UUID adminId) {
        log.info("Redis Cache EVICT [programDashboards] - Assigning session representative for scopeId={}", request.getScopeId());

        RepresentativeAssignment assignment = RepresentativeAssignment.builder()
                .userId(request.getUserId())
                .roles(Set.of(UserRole.SESSION_REP))
                .scopeType(ScopeType.SESSION)
                .scopeId(request.getScopeId())
                .isActive(true)
                .assignedBy(adminId)
                .assignedAt(LocalDateTime.now())
                .build();
        representativeRepository.save(assignment);

        int programRoleId = 5;
        emitRoleAssignment.sendEmittedRoleAssignmentToKafka(request.getUserId(), programRoleId, request.getScopeId());
    }

    /**
     * READ CACHE: Caches complex dashboards comprising secondary Feign requests to User and Auth services.
     */
    @Override
    @Cacheable(value = "programDashboards", key = "#programId") // 🟢 CACHES HEAVY COMPUTATION READS
    public List<SessionDashboardDTO> getProgramSessionsDashboard(UUID programId) {
        log.info("Redis Cache MISS - Loading program sessions dashboard data from DB & Remote Feign for programId={}", programId);

        List<AcademicSession> sessions = academicSessionRepository.findByProgramId(programId);

        return sessions.stream().map(session -> {
            Optional<RepresentativeAssignment> assignment = representativeRepository
                    .findByScopeIdAndScopeTypeAndIsActiveTrue(session.getId(), ScopeType.SESSION);

            String repName = null;
            String repEmail = null;

            if (assignment.isPresent()) {
                UUID studentId = assignment.get().getUserId();
                try {
                    repName = userServiceClient.getStudentName(studentId);
                    UserInternalInfoDTO authInfo = authClient.getAuthUserInfo(studentId);
                    repEmail = (authInfo != null) ? authInfo.getEmail() : "N/A";
                } catch (Exception e) {
                    log.warn("Session representative details merge failed: user profile missing or unavailable");
                    repName = "Profile Pending";
                    repEmail = "N/A";
                }
            }

            return new SessionDashboardDTO(
                    session.getId(),
                    session.getName(),
                    session.getBatchRange(),
                    repName,
                    repEmail,
                    session.getIsActive()
            );
        }).toList();
    }

    /**
     * READ CACHE: Caches representatives list linked to this program structure.
     */
    @Override
    @Cacheable(value = "programReps", key = "#programId") // 🟢 READ CACHE FOR REPS DIRECTORIES
    public List<SessionRepDetailsDTO> getAllSessionRepsByProgram(UUID programId) {
        log.info("Redis Cache MISS - Fetching all session representatives for programId={}", programId);

        List<AcademicSession> sessions = academicSessionRepository.findByProgramId(programId);
        List<UUID> sessionIds = sessions.stream().map(AcademicSession::getId).toList();

        if (sessionIds.isEmpty()) return Collections.emptyList();

        List<RepresentativeAssignment> assignments = representativeRepository
                .findByScopeIdInAndScopeTypeAndIsActiveTrue(sessionIds, ScopeType.SESSION);

        List<UUID> userIds = assignments.stream()
                .map(RepresentativeAssignment::getUserId)
                .distinct()
                .toList();

        if (userIds.isEmpty()) return Collections.emptyList();

        Map<UUID, StudentDTO> profileMap = userServiceClient.getBulkUserDetails(userIds);

        UserBatchRequest batchRequest = new UserBatchRequest(userIds);
        Map<UUID, UserDetailDTO> authMap = authClient.getUserDetailsBatch(batchRequest)
                .stream().collect(Collectors.toMap(UserDetailDTO::userId, d -> d));

        Map<UUID, AcademicSession> sessionMap = sessions.stream()
                .collect(Collectors.toMap(AcademicSession::getId, s -> s));

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