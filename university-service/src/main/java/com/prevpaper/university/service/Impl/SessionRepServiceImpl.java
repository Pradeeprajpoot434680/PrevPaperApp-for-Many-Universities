package com.prevpaper.university.service.Impl;

import com.prevpaper.comman.dto.ContentStatsDTO;
import com.prevpaper.comman.dto.PendingContentDTO;
import com.prevpaper.comman.exception.ResourceAlreadyExist;
import com.prevpaper.comman.exception.ResourceNotFoundException;
import com.prevpaper.university.client.ContentClient;
import com.prevpaper.university.dtos.*;
import com.prevpaper.university.entities.AcademicSession;
import com.prevpaper.university.entities.Program;
import com.prevpaper.university.entities.Semester;
import com.prevpaper.university.entities.Subject;
import com.prevpaper.university.repository.AcademicSessionRepository;
import com.prevpaper.university.repository.ProgramRepository;
import com.prevpaper.university.repository.SemesterRepository;
import com.prevpaper.university.repository.SubjectRepository;
import com.prevpaper.university.service.SessionRepService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict; // 🟢 IMPORTED
import org.springframework.cache.annotation.Cacheable; // 🟢 IMPORTED
import org.springframework.cache.annotation.Caching;   // 🟢 IMPORTED
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionRepServiceImpl implements SessionRepService {

    private final SemesterRepository semesterRepository;
    private final SubjectRepository subjectRepository;
    private final AcademicSessionRepository sessionRepository;
    private final ProgramRepository programRepository;
    private final AcademicSessionRepository academicSessionRepository;
    private final ContentClient contentClient;

    /**
     * MUTATION: Evicts stale structural configuration definitions for the program.
     */
    @Override
    @Transactional
    @CacheEvict(value = "programStructure", key = "#programId") // 🟢 PURGES OLD STRUCTURES
    public Semester addSemester(UUID programId, SemesterRequest request) {
        log.info("Redis Cache EVICT [programStructure] - Adding semester for programId={}", programId);

        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new ResourceNotFoundException("Program not found"));

        boolean isExist = semesterRepository.existsBySemesterNumberAndProgramId(
                request.getSemesterNumber(),
                programId
        );

        if (isExist) {
            throw new ResourceAlreadyExist("Semester " + request.getSemesterNumber() +
                    " already exists for program: " + program.getName());
        }

        Semester semester = Semester.builder()
                .semesterNumber(request.getSemesterNumber())
                .program(program)
                .build();

        return semesterRepository.save(semester);
    }

    /**
     * MUTATION: Evicts cached subjects for this semester so new entries load immediately.
     */
    @Override
    @Transactional
    @CacheEvict(value = "subjects", key = "#semesterId") // 🟢 PURGES OLD SUBJECTS DEFINITIONS
    public Subject addSubject(UUID semesterId, SubjectRequest request) {
        log.info("Redis Cache EVICT [subjects] - Adding subject for semesterId={}", semesterId);

        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> new ResourceNotFoundException("Semester not found with ID: " + semesterId));

        if (subjectRepository.existsBySubjectCode(request.getSubjectCode())) {
            throw new ResourceAlreadyExist("Subject code '" + request.getSubjectCode() + "' is already assigned to another subject.");
        }

        if (subjectRepository.existsByNameIgnoreCaseAndSemesterId(request.getName().trim(), semesterId)) {
            throw new ResourceAlreadyExist("A subject named '" + request.getName() + "' already exists in this semester.");
        }

        Subject subject = Subject.builder()
                .name(request.getName().trim())
                .subjectCode(request.getSubjectCode().trim().toUpperCase())
                .semester(semester)
                .build();

        return subjectRepository.save(subject);
    }

    @Override
    public List<SubjectResourceDTO> getSubjectsWithStats(UUID sessionId, Integer semesterNumber) {
        AcademicSession session = academicSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        Semester semester = semesterRepository.findByProgramIdAndSemesterNumber(
                session.getProgram().getId(),
                semesterNumber
        ).orElseThrow(() -> new ResourceNotFoundException("Semester " + semesterNumber + " is not defined for this program"));

        List<Subject> subjects = subjectRepository.findBySemesterId(semester.getId());

        if (subjects.isEmpty()) return Collections.emptyList();

        return subjects.stream()
                .map(subject -> new SubjectResourceDTO(subject.getId(), subject.getName(), subject.getSubjectCode()))
                .toList();
    }

    /**
     * READ CACHE: Caches the subjects array linked globally to a unique semester ID.
     */
    @Override
    @Cacheable(value = "subjects", key = "#semesterId") // 🟢 READ CACHE FOR SUBJECT DIRECTORIES
    public List<SubjectResourceDTO> getSubjectsBySemesterId(UUID semesterId) {
        log.info("Redis Cache MISS - Loading subjects from DB for semesterId={}", semesterId);
        List<Subject> subjects = subjectRepository.findBySemesterId(semesterId);

        return subjects.stream()
                .map(subject -> new SubjectResourceDTO(subject.getId(), subject.getName(), subject.getSubjectCode()))
                .toList();
    }

    /**
     * READ CACHE: Caches complex representative dashboard data structures to eliminate database and Feign overhead.
     */
    @Override
    @Cacheable(value = "sessionDashboards", key = "#sessionId") // 🟢 READ CACHE FOR SESSIONS DASHBOARDS
    public SessionRepDashboardDTO getSessionDashboard(UUID sessionId) {
        log.info("Redis Cache MISS - Computing session dashboard data from DB & Content Feign for sessionId={}", sessionId);

        AcademicSession session = academicSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        Program program = session.getProgram();
        List<Semester> semesters = semesterRepository.findByProgramId(program.getId());

        List<SemesterStatsDTO> semesterStats = semesters.stream().map(sem -> {
            long subjectCount = subjectRepository.countBySemesterId(sem.getId());

            ContentStatsDTO contentStats = contentClient.getSemesterStats(
                    session.getProgram().getId(),
                    sem.getSemesterNumber()
            );

            return new SemesterStatsDTO(
                    sem.getSemesterNumber(),
                    sem.getId(),
                    (int) subjectCount,
                    contentStats.getPendingCount(),
                    contentStats.getVerifiedCount()
            );
        }).sorted(Comparator.comparing(SemesterStatsDTO::semesterNumber)).toList();

        return new SessionRepDashboardDTO(
                session.getId(),
                session.getName(),
                program.getName(),
                semesterStats
        );
    }

    /**
     * READ CACHE: Caches the unverified document data array lists for document verifiers (CRs).
     */
    @Override
    @Cacheable(value = "pendingContent", key = "#sessionId") // 🟢 READ CACHE FOR PENDING TASKS LISTS
    public List<PendingContentDTO> getPendingContentBySession(UUID sessionId) {
        log.info("Redis Cache MISS - Fetching pending verifications content array from Content Feign for sessionId={}", sessionId);

        AcademicSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        return contentClient.getPendingBySession(
                session.getProgram().getId(),
                session.getStartYear()
        );
    }

    /**
     * MUTATION: Evicts pending verification cache records so state updates are accurately reflected.
     */
    @Override
    @Caching(evict = {
            @CacheEvict(value = "pendingContent", allEntries = true),
            @CacheEvict(value = "sessionDashboards", allEntries = true)
    }) // 🟢 FLUSHES CONCURRENT MANAGEMENT CACHES UPON CONTENT AUDITS
    public void updateContentStatus(UUID contentId, UUID repId, VerifyContentRequest request) {
        log.info("Redis Cache EVICT [pendingContent, sessionDashboards] - Auditing verification status for contentId={}", contentId);
        contentClient.updateStatus(contentId, request.status(), repId);
    }

    /**
     * MUTATION: Evicts operational caches when an asset is deleted.
     */
    @Override
    @Caching(evict = {
            @CacheEvict(value = "pendingContent", allEntries = true),
            @CacheEvict(value = "sessionDashboards", allEntries = true)
    }) // 🟢 PURGES DATA LAYOUT HOOKS UPON ARCHIVE REMOVALS
    public void deleteContent(UUID contentId) {
        log.info("Redis Cache EVICT [pendingContent, sessionDashboards] - Removing content asset reference for contentId={}", contentId);
        contentClient.deleteById(contentId);
    }
}