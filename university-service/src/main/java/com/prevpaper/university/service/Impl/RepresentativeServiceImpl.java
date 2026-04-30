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
import org.springframework.stereotype.Service;


import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor

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




    @Override
    public List<ProgramDashboardDTO> getDepartmentProgramsDashboard(UUID departmentId) {
        // 1. Fetch all programs for this department
        List<Program> programs = programRepository.findByDepartmentId(departmentId);

        return programs.stream().map(prog -> {
            // 2. Find active assignment for this PROGRAM scope
            Optional<RepresentativeAssignment> assignment = representativeRepository
                    .findByScopeIdAndScopeTypeAndIsActiveTrue(prog.getId(), ScopeType.PROGRAM);

            String repName = null;
            if (assignment.isPresent()) {
                try {
                    repName = userServiceClient.getStudentName(assignment.get().getUserId());
                } catch (Exception e) {
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
        // Fetch profiles from User Service
        List<StudentDTO> studentProfiles = userServiceClient.getStudentsByDept(deptId);

        // Enrich with Emails from Auth Service
        return enrichStudentsWithEmails(studentProfiles);
    }

    @Override
    public List<StudentDTO> getStudentsByProgram(UUID programId) {
        // Fetch profiles from User Service
        List<StudentDTO> studentProfiles = userServiceClient.getStudentsByProgram(programId);

        // Enrich with Emails from Auth Service
        return enrichStudentsWithEmails(studentProfiles);
    }


    private List<StudentDTO> enrichStudentsWithEmails(List<StudentDTO> students) {
        if (students == null || students.isEmpty()) {
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
            return students.stream().map(s -> new StudentDTO(
                    s.authUserId(),
                    s.fullName(),
                    "Email Service Unavailable"
            )).toList();
        }
    }


    @Override
    public List<RepresentativeDetailsDTO> getProgramRepsByDept(UUID departmentId) {
        // 1. Get all programs in this department
        List<Program> programs = programRepository.findByDepartmentId(departmentId);
        List<UUID> programIds = programs.stream().map(Program::getId).toList();

        if (programIds.isEmpty()) return Collections.emptyList();

        // 2. Find all active assignments for these programs
        List<RepresentativeAssignment> assignments = representativeRepository
                .findByScopeIdInAndScopeTypeAndIsActiveTrue(programIds, ScopeType.PROGRAM);

        // 3. Collect User IDs for enrichment
        List<UUID> userIds = assignments.stream()
                .map(RepresentativeAssignment::getUserId)
                .distinct()
                .toList();

        // 4. Batch Fetch Names and Emails
        Map<UUID, StudentDTO> profileMap = userServiceClient.getBulkUserDetails(userIds);
        UserBatchRequest batchRequest = new UserBatchRequest(userIds);
        Map<UUID, UserDetailDTO> authMap = authClient.getUserDetailsBatch(batchRequest)
                .stream().collect(Collectors.toMap(UserDetailDTO::userId, d -> d));

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