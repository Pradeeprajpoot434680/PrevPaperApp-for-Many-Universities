package com.prevpaper.university.service.Impl;
import com.prevpaper.comman.dto.StudentDTO;
import com.prevpaper.comman.dto.UserBatchRequest;
import com.prevpaper.comman.dto.UserDetailDTO;
import com.prevpaper.comman.dto.UserInternalInfoDTO;
import com.prevpaper.comman.enums.ScopeType;
import com.prevpaper.comman.exception.ResourceNotFoundException;
import com.prevpaper.university.client.AuthClient;
import com.prevpaper.university.client.ContentClient;
import com.prevpaper.university.client.UserServiceClient;
import com.prevpaper.university.dtos.*;
import com.prevpaper.university.entities.*;
import com.prevpaper.university.repository.*;
import com.prevpaper.university.service.RepresentativeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private  final SemesterRepository semesterRepository;
    private final SubjectRepository subjectRepository;
    private final ContentClient contentClient;

    @Override
    public List<DepartmentRepResponse> getDeptRepsByUniversity(UUID universityId) {
        log.info("Department representatives by university request received: universityId={}", universityId);

        List<RepresentativeAssignment> assignments =
                representativeRepository.findByScopeTypeAndIsActiveTrue(ScopeType.DEPARTMENT);
        log.info("Active department representative assignments loaded: assignmentCount={}", assignments.size());

        Map<UUID, String> deptNames = departmentRepo.findByUniversityId(universityId)
                .stream()
                .collect(Collectors.toMap(Department::getId, Department::getName));

        List<RepresentativeAssignment> uniAssignments = assignments.stream()
                .filter(a -> deptNames.containsKey(a.getScopeId()))
                .toList();
        log.info("Department representative assignments filtered by university: universityId={}, assignmentCount={}",
                universityId, uniAssignments.size());

        if (uniAssignments.isEmpty()) {
            log.info("No department representatives found for university: universityId={}", universityId);
            return Collections.emptyList();
        }

        List<UUID> userIds = uniAssignments.stream()
                .map(RepresentativeAssignment::getUserId)
                .toList();

        List<UserDetailDTO> userDetails =
                authClient.getUserDetailsBatch(new UserBatchRequest(userIds));
        log.info("Department representative auth details loaded: universityId={}, requestedUsers={}, receivedAuthDetails={}",
                universityId, userIds.size(), userDetails.size());

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




    @Override
    public List<ProgramDashboardDTO> getDepartmentProgramsDashboard(UUID departmentId) {
        log.info("Department programs dashboard request received: departmentId={}", departmentId);
        // 1. Fetch all programs for this department
        List<Program> programs = programRepository.findByDepartmentId(departmentId);
        log.info("Programs loaded for department dashboard: departmentId={}, programCount={}",
                departmentId, programs.size());

        return programs.stream().map(prog -> {
            // 2. Find active assignment for this PROGRAM scope
            Optional<RepresentativeAssignment> assignment = representativeRepository
                    .findByScopeIdAndScopeTypeAndIsActiveTrue(prog.getId(), ScopeType.PROGRAM);

            String repName = null;
            if (assignment.isPresent()) {
                try {
                    repName = userServiceClient.getStudentName(assignment.get().getUserId());
                } catch (Exception e) {
                    log.warn("Program dashboard representative profile lookup failed: departmentId={}, programId={}, repUserId={}, error={}",
                            departmentId, prog.getId(), assignment.get().getUserId(), e.getMessage());
                    repName = "Profile Pending";
                }
            }

            return new ProgramDashboardDTO(
                    prog.getId(),
                    prog.getName(),
                    prog.getCode(),
                    "4 Years", // You can calculate this or add a field to Program entity
                    repName,
                    0L, // Logic to fetch student count from User Service can be added later
                    true
            );
        }).toList();
    }


    @Override
    public List<StudentDTO> getStudentsByDepartment(UUID deptId) {
        log.info("Students by department request received: departmentId={}", deptId);
        // Fetch profiles from User Service
        List<StudentDTO> studentProfiles = userServiceClient.getStudentsByDept(deptId);
        log.info("Student profiles loaded by department: departmentId={}, profileCount={}",
                deptId, studentProfiles == null ? 0 : studentProfiles.size());

        // Enrich with Emails from Auth Service
        return enrichStudentsWithEmails(studentProfiles);
    }

    @Override
    public List<StudentDTO> getStudentsByProgram(UUID programId) {
        log.info("Students by program request received: programId={}", programId);
        // Fetch profiles from User Service
        List<StudentDTO> studentProfiles = userServiceClient.getStudentsByProgram(programId);
        log.info("Student profiles loaded by program: programId={}, profileCount={}",
                programId, studentProfiles == null ? 0 : studentProfiles.size());

        // Enrich with Emails from Auth Service
        return enrichStudentsWithEmails(studentProfiles);
    }


    private List<StudentDTO> enrichStudentsWithEmails(List<StudentDTO> students) {
        if (students == null || students.isEmpty()) {
            log.info("Student email enrichment skipped: no students");
            return Collections.emptyList();
        }

        // 1. Collect all unique User IDs from the profiles
        List<UUID> userIds = students.stream()
                .map(StudentDTO::authUserId)
                .distinct()
                .toList();

        // 2. Batch fetch Auth details (Emails) from Auth-Service
        // Using your existing getUserDetailsBatch in AuthClient
        try {
            UserBatchRequest batchRequest = new UserBatchRequest(userIds);
            List<UserDetailDTO> authDetails = authClient.getUserDetailsBatch(batchRequest);
            log.info("Student email enrichment auth details loaded: requestedUsers={}, receivedAuthDetails={}",
                    userIds.size(), authDetails.size());

            // 3. Map emails by userId for O(1) lookup
            Map<UUID, String> emailMap = authDetails.stream()
                    .collect(Collectors.toMap(UserDetailDTO::userId, UserDetailDTO::email));

            // 4. Merge the email into the DTOs
            return students.stream().map(s -> new StudentDTO(
                    s.authUserId(),
                    s.fullName(),
                    emailMap.getOrDefault(s.authUserId(), "Email Not Found")
            )).toList();

        } catch (Exception e) {
            // Fallback: If Auth-Service is down, return students with "N/A" emails instead of crashing
            log.warn("Student email enrichment failed, using fallback email value: requestedUsers={}, error={}",
                    userIds.size(), e.getMessage());
            return students.stream().map(s -> new StudentDTO(
                    s.authUserId(),
                    s.fullName(),
                    "Email Service Unavailable"
            )).toList();
        }
    }


    @Override
    public List<RepresentativeDetailsDTO> getProgramRepsByDept(UUID departmentId) {
        log.info("Program representatives by department request received: departmentId={}", departmentId);
        // 1. Get all programs in this department
        List<Program> programs = programRepository.findByDepartmentId(departmentId);
        List<UUID> programIds = programs.stream().map(Program::getId).toList();
        log.info("Programs loaded for representative lookup: departmentId={}, programCount={}",
                departmentId, programs.size());

        if (programIds.isEmpty()) {
            log.info("No programs found for representative lookup: departmentId={}", departmentId);
            return Collections.emptyList();
        }

        // 2. Find all active assignments for these programs
        List<RepresentativeAssignment> assignments = representativeRepository
                .findByScopeIdInAndScopeTypeAndIsActiveTrue(programIds, ScopeType.PROGRAM);
        log.info("Program representative assignments loaded: departmentId={}, assignmentCount={}",
                departmentId, assignments.size());

        // 3. Collect User IDs for enrichment
        List<UUID> userIds = assignments.stream()
                .map(RepresentativeAssignment::getUserId)
                .distinct()
                .toList();

        // 4. Batch Fetch Names and Emails
        Map<UUID, StudentDTO> profileMap = userServiceClient.getBulkUserDetails(userIds);
        log.info("Program representative profile details loaded: requestedUsers={}, receivedProfiles={}",
                userIds.size(), profileMap.size());
        UserBatchRequest batchRequest = new UserBatchRequest(userIds);
        Map<UUID, UserDetailDTO> authMap = authClient.getUserDetailsBatch(batchRequest)
                .stream().collect(Collectors.toMap(UserDetailDTO::userId, d -> d));
        log.info("Program representative auth details loaded: requestedUsers={}, receivedAuthDetails={}",
                userIds.size(), authMap.size());

        // 5. Build the final list
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
                    prog.getId(),
                    prog.getName(),
                    prog.getCode(),
                    rep.getAssignedAt(),
                    rep.getIsActive()
            );
        }).toList();
    }


}
