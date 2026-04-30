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
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
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

        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new ResourceNotFoundException("Program not found"));

        // Check if Semester exists for THIS specific program
        boolean isExist = semesterRepository.existsBySemesterNumberAndProgramId(
                request.getSemesterNumber(),
                programId
        );

        if(isExist){
            throw new ResourceAlreadyExist("Semester " + request.getSemesterNumber() +
                    " already exists for program: " + program.getName());
        }

        Semester semester = Semester.builder()
                .semesterNumber(request.getSemesterNumber())
                .program(program)
                .build();

        return semesterRepository.save(semester);
    }

    @Override
    @Transactional
    public Subject addSubject(UUID semesterId, SubjectRequest request) {

        // 1. Verify Semester exists
        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> new ResourceNotFoundException("Semester not found with ID: " + semesterId));

        // 2. Validation: Check if Subject Code already exists globally
        if (subjectRepository.existsBySubjectCode(request.getSubjectCode())) {
            throw new ResourceAlreadyExist("Subject code '" + request.getSubjectCode() + "' is already assigned to another subject.");
        }

        // 3. Validation: Check if a subject with the same name exists in THIS semester
        if (subjectRepository.existsByNameIgnoreCaseAndSemesterId(request.getName().trim(), semesterId)) {
            throw new ResourceAlreadyExist("A subject named '" + request.getName() + "' already exists in this semester.");
        }

        // 4. Build and Save
        Subject subject = Subject.builder()
                .name(request.getName().trim())
                .subjectCode(request.getSubjectCode().trim().toUpperCase()) // Normalize code to Uppercase
                .semester(semester)
                .build();

        return subjectRepository.save(subject);
    }

    @Override
    public List<SubjectResourceDTO> getSubjectsWithStats(UUID sessionId, Integer semesterNumber) {
        // 1. Find the Session to identify the Program context
        AcademicSession session = academicSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        // 2. Find the Semester for this specific Program
        // This ensures we aren't fetching subjects from a different department's semester
        Semester semester = semesterRepository.findByProgramIdAndSemesterNumber(
                session.getProgram().getId(),
                semesterNumber
        ).orElseThrow(() -> new ResourceNotFoundException("Semester " + semesterNumber + " is not defined for this program"));

        // 3. Get all Subjects linked to this Semester
        List<Subject> subjects = subjectRepository.findBySemesterId(semester.getId());

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
        // We skip the session/program check here because the UUID is unique globally
        List<Subject> subjects = subjectRepository.findBySemesterId(semesterId);

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
        // 1. Fetch Session and Program info
        AcademicSession session = academicSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        Program program = session.getProgram();

        // 2. Fetch all Semesters for this Program
        List<Semester> semesters = semesterRepository.findByProgramId(program.getId());

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
        // Call Content-Service via Feign Client
        return contentClient.getPendingByScope(sessionId);
    }
}