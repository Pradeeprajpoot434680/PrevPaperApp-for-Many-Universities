package com.prevpaper.university.service.Impl;

import com.prevpaper.comman.dto.ContentStatsDTO;
import com.prevpaper.comman.dto.PendingContentDTO;
import com.prevpaper.comman.exception.ResourceAlreadyExist;
import com.prevpaper.comman.exception.ResourceNotFoundException;
import com.prevpaper.comman.exception.RoleExceptionHandler;
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
    @Override
    @Transactional
    public Semester addSemester( UUID programId, SemesterRequest request) {
        log.info("Add semester request received: programId={}, semesterNumber={}",
                programId, request.getSemesterNumber());

        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new ResourceNotFoundException("Program not found"));
        log.info("Add semester program resolved: programId={}, programName={}", program.getId(), program.getName());

        // Check if Semester exists for THIS specific program
        boolean isExist = semesterRepository.existsBySemesterNumberAndProgramId(
                request.getSemesterNumber(),
                programId
        );

        if(isExist){
            log.warn("Add semester rejected: duplicate semester, programId={}, semesterNumber={}",
                    programId, request.getSemesterNumber());
            throw new ResourceAlreadyExist("Semester " + request.getSemesterNumber() +
                    " already exists for program: " + program.getName());
        }

        Semester semester = Semester.builder()
                .semesterNumber(request.getSemesterNumber())
                .program(program)
                .build();

        Semester savedSemester = semesterRepository.save(semester);
        log.info("Semester added: semesterId={}, programId={}, semesterNumber={}",
                savedSemester.getId(), programId, savedSemester.getSemesterNumber());
        return savedSemester;
    }

    @Override
    @Transactional
    public Subject addSubject(UUID semesterId, SubjectRequest request) {
        log.info("Add subject request received: semesterId={}, name={}, subjectCode={}",
                semesterId, request.getName(), request.getSubjectCode());

        // 1. Verify Semester exists
        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> new ResourceNotFoundException("Semester not found with ID: " + semesterId));
        log.info("Add subject semester resolved: semesterId={}, programId={}, semesterNumber={}",
                semester.getId(), semester.getProgram().getId(), semester.getSemesterNumber());

        // 2. Validation: Check if Subject Code already exists globally
        if (subjectRepository.existsBySubjectCode(request.getSubjectCode())) {
            log.warn("Add subject rejected: duplicate subject code, semesterId={}, subjectCode={}",
                    semesterId, request.getSubjectCode());
            throw new ResourceAlreadyExist("Subject code '" + request.getSubjectCode() + "' is already assigned to another subject.");
        }

        // 3. Validation: Check if a subject with the same name exists in THIS semester
        if (subjectRepository.existsByNameIgnoreCaseAndSemesterId(request.getName().trim(), semesterId)) {
            log.warn("Add subject rejected: duplicate subject name in semester, semesterId={}, name={}",
                    semesterId, request.getName());
            throw new ResourceAlreadyExist("A subject named '" + request.getName() + "' already exists in this semester.");
        }

        // 4. Build and Save
        Subject subject = Subject.builder()
                .name(request.getName().trim())
                .subjectCode(request.getSubjectCode().trim().toUpperCase()) // Normalize code to Uppercase
                .semester(semester)
                .build();

        Subject savedSubject = subjectRepository.save(subject);
        log.info("Subject added: subjectId={}, semesterId={}, name={}, subjectCode={}",
                savedSubject.getId(), semesterId, savedSubject.getName(), savedSubject.getSubjectCode());
        return savedSubject;
    }

    @Override
    public List<SubjectResourceDTO> getSubjectsWithStats(UUID sessionId, Integer semesterNumber) {
        log.info("Get subjects with stats request received: sessionId={}, semesterNumber={}",
                sessionId, semesterNumber);
        // 1. Find the Session to identify the Program context
        AcademicSession session = academicSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
        log.info("Subjects with stats session resolved: sessionId={}, programId={}",
                session.getId(), session.getProgram().getId());

        // 2. Find the Semester for this specific Program
        // This ensures we aren't fetching subjects from a different department's semester
        Semester semester = semesterRepository.findByProgramIdAndSemesterNumber(
                session.getProgram().getId(),
                semesterNumber
        ).orElseThrow(() -> new ResourceNotFoundException("Semester " + semesterNumber + " is not defined for this program"));

        // 3. Get all Subjects linked to this Semester
        List<Subject> subjects = subjectRepository.findBySemesterId(semester.getId());
        log.info("Subjects loaded with stats context: sessionId={}, semesterId={}, semesterNumber={}, subjectCount={}",
                sessionId, semester.getId(), semesterNumber, subjects.size());

        if (subjects.isEmpty()) {
            return Collections.emptyList();
        }

        // 4. Map entities to the simplified DTO
        return subjects.stream()
                .map(subject -> new SubjectResourceDTO(
                        subject.getId(),
                        subject.getName(),
                        subject.getSubjectCode()
                ))
                .toList();
    }



    @Override
    public List<SubjectResourceDTO> getSubjectsBySemesterId(UUID semesterId) {
        log.info("Get subjects by semester request received: semesterId={}", semesterId);
        // We skip the session/program check here because the UUID is unique globally
        List<Subject> subjects = subjectRepository.findBySemesterId(semesterId);
        log.info("Subjects by semester loaded: semesterId={}, subjectCount={}", semesterId, subjects.size());

        return subjects.stream()
                .map(subject -> new SubjectResourceDTO(
                        subject.getId(),
                        subject.getName(),
                        subject.getSubjectCode()
                ))
                .toList();
    }

    @Override
    public SessionRepDashboardDTO getSessionDashboard(UUID sessionId) {
        log.info("Session representative dashboard request received: sessionId={}", sessionId);
        // 1. Fetch Session and Program info
        AcademicSession session = academicSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        Program program = session.getProgram();

        // 2. Fetch all Semesters for this Program
        List<Semester> semesters = semesterRepository.findByProgramId(program.getId());
        log.info("Session dashboard base data loaded: sessionId={}, programId={}, semesterCount={}",
                sessionId, program.getId(), semesters.size());

        // 3. Prepare the Semester Stats List
        List<SemesterStatsDTO> semesterStats = semesters.stream().map(sem -> {
            // Count subjects locally in University-Service
            long subjectCount = subjectRepository.countBySemesterId(sem.getId());

            // 4. Feign Call to Content-Service for paper counts
            // Note: You can also batch this call outside the loop for better performance
            ContentStatsDTO contentStats = contentClient.getSemesterStats(
                    session.getProgram().getId(),
                    sem.getSemesterNumber()
            );
            log.info("Content stats loaded for session dashboard: sessionId={}, programId={}, semesterNumber={}, pending={}, verified={}",
                    sessionId, session.getProgram().getId(), sem.getSemesterNumber(),
                    contentStats.getPendingCount(), contentStats.getVerifiedCount());

            return new SemesterStatsDTO(
                    sem.getSemesterNumber(),
                    sem.getId(),
                    (int) subjectCount,
                    contentStats.getPendingCount(),
                    contentStats.getVerifiedCount()
            );
        }).sorted(Comparator.comparing(SemesterStatsDTO::semesterNumber)).toList();

        // 5. Build and return the final Dashboard DTO
        return new SessionRepDashboardDTO(
                session.getId(),
                session.getName(), // "Batch 2022"
                program.getName(),
                semesterStats
        );
    }

    @Override
    public List<PendingContentDTO> getPendingContentBySession(UUID sessionId) {
        log.info("Pending content by session request received: sessionId={}", sessionId);
        // 1. Look up the Session to find its Program and Year
        AcademicSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        log.info("Pending content session resolved: sessionId={}, programId={}, startYear={}",
                session.getId(), session.getProgram().getId(), session.getStartYear());

        // 2. Fetch unverified papers from Content Service using those filters
        List<PendingContentDTO> pendingContent = contentClient.getPendingBySession(
                session.getProgram().getId(),
                session.getStartYear()
        );
        log.info("Pending content loaded: sessionId={}, programId={}, startYear={}, pendingCount={}",
                sessionId, session.getProgram().getId(), session.getStartYear(), pendingContent.size());
        return pendingContent;
    }


    @Override
    public void updateContentStatus(UUID contentId, UUID repId, VerifyContentRequest request) {
        // Logic: Tell Content Service to update status and set the verifier ID
        log.info("Update content status request received: contentId={}, repId={}, status={}",
                contentId, repId, request.status());
        contentClient.updateStatus(contentId, request.status(), repId);
        log.info("Update content status request sent to content service: contentId={}, repId={}, status={}",
                contentId, repId, request.status());
    }

    @Override
    public void deleteContent(UUID contentId) {
        log.info("Delete content request received: contentId={}", contentId);
        contentClient.deleteById(contentId);
        log.info("Delete content request sent to content service: contentId={}", contentId);
    }
}
