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
import org.springframework.cache.annotation.Cacheable; // 🟢 IMPORTED
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.prevpaper.comman.enums.ScopeType.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class UniversityDiscoveryServiceImpl implements UniversityDiscoveryService {

    private final DepartmentRepository departmentRepository;
    private final ProgramRepository programRepository;
    private final SemesterRepository semesterRepository;
    private final SubjectRepository subjectRepository;
    private final AcademicSessionRepository sessionRepository;
    private final ExamConfigRepository examRepo;
    private final RepresentativeRepository representativeRepository;
    private final UserServiceClient userServiceClient;

    /**
     * CACHE HIT: Avoids computing complex multi-tier joins and bulk Feign calls
     * across different services for the university team directory.
     */
    @Override
    @Cacheable(value = "universityTeam", key = "#universityId")
    public List<UniversityTeamMemberDTO> getUniversityTeam(UUID universityId) {
        log.info("Redis Cache MISS - Fetching university team directory from DB & Remote Feign: universityId={}", universityId);

        List<Department> departments = departmentRepository.findByUniversityId(universityId);
        List<UUID> deptIds = departments.stream().map(Department::getId).toList();

        List<Program> programs = programRepository.findByDepartmentIdIn(deptIds);
        List<UUID> progIds = programs.stream().map(Program::getId).toList();

        List<AcademicSession> sessions = sessionRepository.findByProgramIdIn(progIds);
        List<UUID> sessionIds = sessions.stream().map(AcademicSession::getId).toList();

        List<UUID> allScopeIds = new ArrayList<>();
        allScopeIds.add(universityId);
        allScopeIds.addAll(deptIds);
        allScopeIds.addAll(progIds);
        allScopeIds.addAll(sessionIds);

        List<RepresentativeAssignment> assignments = representativeRepository.findAllByScopeIdInAndIsActiveTrue(allScopeIds);

        List<UUID> userIds = assignments.stream().map(RepresentativeAssignment::getUserId   ).distinct().toList();
        if (userIds.isEmpty()) return Collections.emptyList();

        // 1. Fetch raw remote map profiles data payload block
        Map<UUID, UserData> rawProfileMap = userServiceClient.getUsersByIds(userIds);

        // 🟢 2. FIXED: Normalize profileMap keys to standard uppercase strings to handle mapping lookups safely
        Map<String, UserData> profileMap = new HashMap<>();
        if (rawProfileMap != null) {
            rawProfileMap.forEach((key, value) -> {
                if (key != null) {
                    profileMap.put(key.toString().toLowerCase(), value);
                }
            });
        }

        Map<UUID, String> deptMap = departments.stream().collect(Collectors.toMap(Department::getId, Department::getName));
        Map<UUID, String> programMap = programs.stream().collect(Collectors.toMap(Program::getId, Program::getName));
        Map<UUID, String> sessionMap = sessions.stream().collect(Collectors.toMap(AcademicSession::getId, AcademicSession::getName));

        return assignments.stream()
                .map(assign -> {
                    // 🟢 3. FIXED: Extract profiles safely using converted lower-case string keys
                    String userLookupKey = assign.getUserId() != null ? assign.getUserId().toString().toLowerCase() : "";
                    UserData profile = profileMap.get(userLookupKey);

                    return new UniversityTeamMemberDTO(
                            assign.getUserId(),
                            profile != null && profile.firstName() != null ? profile.firstName() : "Unknown",
                            profile != null && profile.lastName() != null ? profile.lastName() : "User",
                            profile != null ? profile.profileImageUrl() : null,
                            getPrimaryRole(assign),
                            resolveScopeName(assign, deptMap, programMap, sessionMap),
                            assign.getScopeType()
                    );
                })
                .toList();
    }

    @Override
    @Cacheable(value = "departments", key = "#universityId")
    public List<IdNameDTO> getDepartments(UUID universityId) {
        log.info("Redis Cache MISS - Loading discovery departments from DB for university: {}", universityId);
        return departmentRepository.findByUniversityId(universityId).stream()
                .map(d -> new IdNameDTO(d.getId(), d.getName()))
                .toList();
    }

    @Override
    @Cacheable(value = "programs", key = "#departmentId")
    public List<IdNameDTO> getPrograms(UUID departmentId) {
        log.info("Redis Cache MISS - Loading discovery programs from DB for department: {}", departmentId);
        return programRepository.findByDepartmentId(departmentId).stream()
                .map(p -> new IdNameDTO(p.getId(), p.getName()))
                .toList();
    }

    @Override
    @Cacheable(value = "programStructure", key = "#programId")
    public ProgramStructureDTO getProgramStructure(UUID programId) {
        log.info("Redis Cache MISS - Computing program semesters & batch sessions from DB for program: {}", programId);
        List<IdNameDTO> semesters = semesterRepository.findByProgramId(programId).stream()
                .map(s -> new IdNameDTO(s.getId(), "Semester " + s.getSemesterNumber()))
                .toList();

        List<IdNameDTO> sessions = sessionRepository.findByProgramId(programId).stream()
                .map(s -> new IdNameDTO(s.getId(), s.getName()))
                .toList();

        return new ProgramStructureDTO(semesters, sessions);
    }

    @Override
    @Cacheable(value = "subjects", key = "#semesterId")
    public List<IdNameDTO> getSubjects(UUID semesterId) {
        log.info("Redis Cache MISS - Loading subjects from DB for semester: {}", semesterId);
        return subjectRepository.findBySemesterId(semesterId).stream()
                .map(s -> new IdNameDTO(s.getId(), s.getName() + " (" + s.getSubjectCode() + ")"))
                .toList();
    }

    @Override
    @Cacheable(value = "examConfigs", key = "#universityId")
    public List<IdNameDTO> getExamConfigs(UUID universityId) {
        log.info("Redis Cache MISS - Loading active exam configurations from DB for university: {}", universityId);
        return examRepo.findByUniversityIdAndIsActiveTrue(universityId).stream()
                .map(e -> new IdNameDTO(e.getId(), e.getName()))
                .toList();
    }

    private String getPrimaryRole(RepresentativeAssignment assign) {
        return assign.getRoles() != null && !assign.getRoles().isEmpty()
                ? assign.getRoles().iterator().next().name()
                : "MEMBER";
    }

    private String resolveScopeName(RepresentativeAssignment assign, Map<UUID, String> deptMap, Map<UUID, String> programMap, Map<UUID, String> sessionMap) {
        UUID scopeId = assign.getScopeId();
        return switch (assign.getScopeType()) {
            case UNIVERSITY -> "University Admin";
            case DEPARTMENT -> deptMap.getOrDefault(scopeId, "Unknown Dept");
            case PROGRAM -> programMap.getOrDefault(scopeId, "Unknown Program");
            case SESSION -> sessionMap.getOrDefault(scopeId, "Unknown Session");
        };
    }
}