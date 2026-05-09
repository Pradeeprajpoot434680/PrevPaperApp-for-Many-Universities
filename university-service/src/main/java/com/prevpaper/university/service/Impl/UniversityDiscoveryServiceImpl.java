package com.prevpaper.university.service.Impl;

import com.prevpaper.comman.dto.UserData;
import com.prevpaper.comman.enums.ScopeType;
import com.prevpaper.university.client.UserServiceClient;
import com.prevpaper.university.dtos.IdNameDTO;
import com.prevpaper.university.dtos.ProgramStructureDTO;
import com.prevpaper.university.dtos.UniversityTeamMemberDTO;
import com.prevpaper.university.entities.AcademicSession;
import com.prevpaper.university.entities.Department;
import com.prevpaper.university.entities.Program;
import com.prevpaper.university.entities.RepresentativeAssignment;
import com.prevpaper.university.repository.*;
import com.prevpaper.university.service.UniversityDiscoveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.prevpaper.comman.enums.ScopeType.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class UniversityDiscoveryServiceImpl implements UniversityDiscoveryService {

    private final UniversityRepository universityRepository;
    private final DepartmentRepository departmentRepository;
    private final ProgramRepository programRepository;
    private final SemesterRepository semesterRepository;
    private final SubjectRepository subjectRepository;
    private final AcademicSessionRepository sessionRepository;
    private final ExamConfigRepository examRepo;
    private final RepresentativeRepository representativeRepository;
    private final  UserServiceClient userServiceClient;

    @Override
    public List<UniversityTeamMemberDTO> getUniversityTeam(UUID universityId) {
        log.info("University team request received: universityId={}", universityId);

        // 1. Fetch academic hierarchy
        List<Department> departments = departmentRepository.findByUniversityId(universityId);
        List<UUID> deptIds = departments.stream()
                .map(Department::getId)
                .toList();

        List<Program> programs = programRepository.findByDepartmentIdIn(deptIds);
        List<UUID> progIds = programs.stream()
                .map(Program::getId)
                .toList();

        List<AcademicSession> sessions = sessionRepository.findByProgramIdIn(progIds);
        List<UUID> sessionIds = sessions.stream()
                .map(AcademicSession::getId)
                .toList();
        log.info("University team academic hierarchy loaded: universityId={}, departmentCount={}, programCount={}, sessionCount={}",
                universityId, departments.size(), programs.size(), sessions.size());

        // 2. Combine all scope IDs
        List<UUID> allScopeIds = new ArrayList<>();
        allScopeIds.add(universityId);
        allScopeIds.addAll(deptIds);
        allScopeIds.addAll(progIds);
        allScopeIds.addAll(sessionIds);

        // 3. Fetch assignments
        List<RepresentativeAssignment> assignments =
                representativeRepository.findAllByScopeIdInAndIsActiveTrue(allScopeIds);
        log.info("University team assignments loaded: universityId={}, scopeCount={}, assignmentCount={}",
                universityId, allScopeIds.size(), assignments.size());

        // 4. Batch fetch users
        List<UUID> userIds = assignments.stream()
                .map(RepresentativeAssignment::getUserId)
                .distinct()
                .toList();

        Map<UUID, UserData> profileMap = userServiceClient.getUsersByIds(userIds);
        log.info("University team profiles loaded: universityId={}, requestedUsers={}, receivedProfiles={}",
                universityId, userIds.size(), profileMap.size());

        // 5. Build lookup maps for fast resolution (IMPORTANT OPTIMIZATION)
        Map<UUID, String> deptMap = departments.stream()
                .collect(Collectors.toMap(Department::getId, Department::getName));

        Map<UUID, String> programMap = programs.stream()
                .collect(Collectors.toMap(Program::getId, Program::getName));

        Map<UUID, String> sessionMap = sessions.stream()
                .collect(Collectors.toMap(AcademicSession::getId, AcademicSession::getName));

        // 6. Build DTOs
        return assignments.stream()
                .map(assign -> {
                    UserData profile = profileMap.get(assign.getUserId());

                    return new UniversityTeamMemberDTO(
                            assign.getUserId(),
                            profile != null ? profile.firstName() : "Unknown",
                            profile != null ? profile.lastName() : "User",
                            profile != null ? profile.profileImageUrl() : null,
                            getPrimaryRole(assign),
                            resolveScopeName(assign, deptMap, programMap, sessionMap),
                            assign.getScopeType()
                    );
                })
                .toList();
    }

    /**
     * Safely get primary role
     */
    private String getPrimaryRole(RepresentativeAssignment assign) {
        return assign.getRoles() != null && !assign.getRoles().isEmpty()
                ? assign.getRoles().iterator().next().name()
                : "MEMBER";
    }

    /**
     * Resolve scope name safely using maps
     */
    private String resolveScopeName(
            RepresentativeAssignment assign,
            Map<UUID, String> deptMap,
            Map<UUID, String> programMap,
            Map<UUID, String> sessionMap
    ) {
        UUID scopeId = assign.getScopeId();

        return switch (assign.getScopeType()) {

            case UNIVERSITY -> "University Admin";

            case DEPARTMENT -> deptMap.getOrDefault(scopeId, "Unknown Dept");

            case PROGRAM -> programMap.getOrDefault(scopeId, "Unknown Program");

            case SESSION -> sessionMap.getOrDefault(scopeId, "Unknown Session");
        };
    }



    public List<IdNameDTO> getDepartments(UUID universityId) {
        log.info("Discovery departments request received: universityId={}", universityId);
        List<IdNameDTO> departments = departmentRepository.findByUniversityId(universityId).stream()
                .map(d -> new IdNameDTO(d.getId(), d.getName()))
                .toList();
        log.info("Discovery departments loaded: universityId={}, departmentCount={}", universityId, departments.size());
        return departments;
    }

    public List<IdNameDTO> getPrograms(UUID departmentId) {
        log.info("Discovery programs request received: departmentId={}", departmentId);
        List<IdNameDTO> programs = programRepository.findByDepartmentId(departmentId).stream()
                .map(p -> new IdNameDTO(p.getId(), p.getName()))
                .toList();
        log.info("Discovery programs loaded: departmentId={}, programCount={}", departmentId, programs.size());
        return programs;
    }

    public ProgramStructureDTO getProgramStructure(UUID programId) {
        log.info("Discovery program structure request received: programId={}", programId);
        List<IdNameDTO> semesters = semesterRepository.findByProgramId(programId).stream()
                .map(s -> new IdNameDTO(s.getId(), "Semester " + s.getSemesterNumber()))
                .toList();

        List<IdNameDTO> sessions = sessionRepository.findByProgramId(programId).stream()
                .map(s -> new IdNameDTO(s.getId(), s.getName())) // e.g., "Batch 2022"
                .toList();
        log.info("Discovery program structure loaded: programId={}, semesterCount={}, sessionCount={}",
                programId, semesters.size(), sessions.size());

        return new ProgramStructureDTO(semesters, sessions);
    }

    public List<IdNameDTO> getSubjects(UUID semesterId) {
        log.info("Discovery subjects request received: semesterId={}", semesterId);
        List<IdNameDTO> subjects = subjectRepository.findBySemesterId(semesterId).stream()
                .map(s -> new IdNameDTO(s.getId(), s.getName() + " (" + s.getSubjectCode() + ")"))
                .toList();
        log.info("Discovery subjects loaded: semesterId={}, subjectCount={}", semesterId, subjects.size());
        return subjects;
    }

    public List<IdNameDTO> getExamConfigs(UUID universityId) {
        log.info("Discovery exam configs request received: universityId={}", universityId);
        List<IdNameDTO> examConfigs = examRepo.findByUniversityIdAndIsActiveTrue(universityId).stream()
                .map(e -> new IdNameDTO(
                        e.getId(),
                        e.getName() // Changed from getName() to getExamName()
                ))
                .toList();
        log.info("Discovery exam configs loaded: universityId={}, examConfigCount={}",
                universityId, examConfigs.size());
        return examConfigs;
    }


}
