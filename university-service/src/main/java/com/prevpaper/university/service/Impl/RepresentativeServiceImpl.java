package com.prevpaper.university.service.Impl;

import com.prevpaper.comman.dto.StudentDTO;
import com.prevpaper.comman.dto.UserBatchRequest;
import com.prevpaper.comman.dto.UserDetailDTO;
import com.prevpaper.comman.enums.ScopeType;
import com.prevpaper.university.client.AuthClient;
import com.prevpaper.university.client.ContentClient;
import com.prevpaper.university.client.UserServiceClient;
import com.prevpaper.university.dtos.*;
import com.prevpaper.university.entities.*;
import com.prevpaper.university.repository.*;
import com.prevpaper.university.service.RepresentativeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable; // 🟢 IMPORTED
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RepresentativeServiceImpl implements RepresentativeService {

    private final RepresentativeRepository representativeRepository;
    private final DepartmentRepository departmentRepo;
    private final AuthClient authClient;
    private final UserServiceClient userServiceClient;
    private final ProgramRepository programRepository;
    private final AcademicSessionRepository academicSessionRepository;
    private final SemesterRepository semesterRepository;
    private final SubjectRepository subjectRepository;
    private final ContentClient contentClient;

    /**
     * READ CACHE: Caches active department representative names and emails by university.
     */
    @Override
    @Cacheable(value = "universityReps", key = "#universityId") // 🟢 CACHED BY UNIVERSITY
    public List<DepartmentRepResponse> getDeptRepsByUniversity(UUID universityId) {
        log.info("Redis Cache MISS - Loading department representatives from DB & Auth Feign for universityId={}", universityId);

        List<RepresentativeAssignment> assignments =
                representativeRepository.findByScopeTypeAndIsActiveTrue(ScopeType.DEPARTMENT);

        Map<UUID, String> deptNames = departmentRepo.findByUniversityId(universityId)
                .stream()
                .collect(Collectors.toMap(Department::getId, Department::getName));

        List<RepresentativeAssignment> uniAssignments = assignments.stream()
                .filter(a -> deptNames.containsKey(a.getScopeId()))
                .toList();

        if (uniAssignments.isEmpty()) return Collections.emptyList();

        List<UUID> userIds = uniAssignments.stream()
                .map(RepresentativeAssignment::getUserId)
                .toList();

        List<UserDetailDTO> userDetails =
                authClient.getUserDetailsBatch(new UserBatchRequest(userIds));

        Map<UUID, UserDetailDTO> userMap = userDetails.stream()
                .collect(Collectors.toMap(UserDetailDTO::userId, u -> u));

        return uniAssignments.stream()
                .map(assign -> {
                    UserDetailDTO user = userMap.get(assign.getUserId());
                    return new DepartmentRepResponse(
                            user != null ? user.email() : "N/A",
                            user != null ? user.fullName() : "Unknown User",
                            deptNames.get(assign.getScopeId())
                    );
                }).toList();
    }

    /**
     * READ CACHE: Caches the department-level programs analytical overview metrics.
     */
    @Override
    @Cacheable(value = "departmentDashboards", key = "#departmentId") // 🟢 CACHED BY DEPARTMENT
    public List<ProgramDashboardDTO> getDepartmentProgramsDashboard(UUID departmentId) {
        log.info("Redis Cache MISS - Loading department programs dashboard layout from DB for departmentId={}", departmentId);

        List<Program> programs = programRepository.findByDepartmentId(departmentId);

        return programs.stream().map(prog -> {
            Optional<RepresentativeAssignment> assignment = representativeRepository
                    .findByScopeIdAndScopeTypeAndIsActiveTrue(prog.getId(), ScopeType.PROGRAM);

            String repName = null;
            if (assignment.isPresent()) {
                try {
                    repName = userServiceClient.getStudentName(assignment.get().getUserId());
                } catch (Exception e) {
                    log.warn("Program representative fallback applied due to profile lookup failure");
                    repName = "Profile Pending";
                }
            }

            return new ProgramDashboardDTO(
                    prog.getId(),
                    prog.getName(),
                    prog.getCode(),
                    "4 Years",
                    repName,
                    0L,
                    true
            );
        }).toList();
    }

    /**
     * READ CACHE: Caches enriched department student roster records to minimize User/Auth cross-talk.
     */
    @Override
    @Cacheable(value = "deptStudents", key = "#deptId") // 🟢 CACHED BY DEPT ID
    public List<StudentDTO> getStudentsByDepartment(UUID deptId) {
        log.info("Redis Cache MISS - Gathering enriched student profile list for departmentId={}", deptId);
        List<StudentDTO> studentProfiles = userServiceClient.getStudentsByDept(deptId);
        return enrichStudentsWithEmails(studentProfiles);
    }

    /**
     * READ CACHE: Caches enriched program student rosters.
     */
    @Override
    @Cacheable(value = "programStudents", key = "#programId") // 🟢 CACHED BY PROGRAM ID
    public List<StudentDTO> getStudentsByProgram(UUID programId) {
        log.info("Redis Cache MISS - Gathering enriched student profile list for programId={}", programId);
        List<StudentDTO> studentProfiles = userServiceClient.getStudentsByProgram(programId);
        return enrichStudentsWithEmails(studentProfiles);
    }

    private List<StudentDTO> enrichStudentsWithEmails(List<StudentDTO> students) {
        if (students == null || students.isEmpty()) return Collections.emptyList();

        List<UUID> userIds = students.stream()
                .map(StudentDTO::authUserId)
                .distinct()
                .toList();

        try {
            UserBatchRequest batchRequest = new UserBatchRequest(userIds);
            List<UserDetailDTO> authDetails = authClient.getUserDetailsBatch(batchRequest);

            Map<UUID, String> emailMap = authDetails.stream()
                    .collect(Collectors.toMap(UserDetailDTO::userId, UserDetailDTO::email));

            return students.stream().map(s -> new StudentDTO(
                    s.authUserId(),
                    s.fullName(),
                    emailMap.getOrDefault(s.authUserId(), "Email Not Found")
            )).toList();

        } catch (Exception e) {
            log.warn("Email service unavailable during profile data enrichment - falling back to safety strings");
            return students.stream().map(s -> new StudentDTO(
                    s.authUserId(),
                    s.fullName(),
                    "Email Service Unavailable"
            )).toList();
        }
    }

    /**
     * READ CACHE: Caches active program representative allocations by department.
     */
    @Override
    @Cacheable(value = "programReps", key = "#departmentId") // 🟢 CACHED BY DEPARTMENT ID
    public List<RepresentativeDetailsDTO> getProgramRepsByDept(UUID departmentId) {
        log.info("Redis Cache MISS - Loading program representatives registry for departmentId={}", departmentId);

        List<Program> programs = programRepository.findByDepartmentId(departmentId);
        List<UUID> programIds = programs.stream().map(Program::getId).toList();

        if (programIds.isEmpty()) return Collections.emptyList();

        List<RepresentativeAssignment> assignments = representativeRepository
                .findByScopeIdInAndScopeTypeAndIsActiveTrue(programIds, ScopeType.PROGRAM);

        List<UUID> userIds = assignments.stream()
                .map(RepresentativeAssignment::getUserId)
                .distinct()
                .toList();

        if (userIds.isEmpty()) return Collections.emptyList();

        Map<UUID, StudentDTO> profileMap = userServiceClient.getBulkUserDetails(userIds);
        UserBatchRequest batchRequest = new UserBatchRequest(userIds);
        Map<UUID, UserDetailDTO> authMap = authClient.getUserDetailsBatch(batchRequest)
                .stream().collect(Collectors.toMap(UserDetailDTO::userId, d -> d));

        Map<UUID, Program> programMap = programs.stream()
                .collect(Collectors.toMap(Program::getId, p -> p));

        return assignments.stream().map(rep -> {
            Program prog = programMap.get(rep.getScopeId());
            StudentDTO profile = profileMap.get(rep.getUserId());
            UserDetailDTO auth = authMap.get(rep.getUserId());

            return new RepresentativeDetailsDTO(
                    rep.getId(),
                    rep.getUserId(),
                    profile != null ? profile.fullName() : "No Profile",
                    auth != null ? auth.email() : "No Email",
                    prog != null ? prog.getId() : null,
                    prog != null ? prog.getName() : "Unknown Program",
                    prog != null ? prog.getCode() : "N/A",
                    rep.getAssignedAt() != null ? rep.getAssignedAt().toString() : null, // 🟢 FIXED: Converted LocalDateTime to String
                    rep.getIsActive()
            );
        }).toList();
    }

    @Override
    @Cacheable(value = "programSessions", key = "#programId") // 🟢 READ CACHE ACTIVATED
    public List<SessionTinyDTO> getSessionsByProgram(UUID programId) {
        log.info("Redis Cache MISS - Loading academic sessions from DB for programId={}", programId);

        // Fetch sessions belonging to this program from your repository
        List<AcademicSession> sessions = academicSessionRepository.findByProgramId(programId);

        // Map to clean serializable DTOs
        return sessions.stream()
                .map(s -> new SessionTinyDTO(
                        s.getId(),
                        s.getStartYear(),
                        s.getEndYear()
                ))
                .toList();
    }
}