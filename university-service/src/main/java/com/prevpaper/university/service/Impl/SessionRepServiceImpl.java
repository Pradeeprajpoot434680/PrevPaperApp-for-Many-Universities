package com.prevpaper.university.service.Impl;

import com.prevpaper.comman.exception.ResourceAlreadyExist;
import com.prevpaper.comman.exception.ResourceNotFoundException;
import com.prevpaper.comman.exception.RoleExceptionHandler;
import com.prevpaper.university.dtos.SemesterRequest;
import com.prevpaper.university.dtos.SubjectRequest;
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

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionRepServiceImpl implements SessionRepService {

    private final SemesterRepository semesterRepository;
    private final SubjectRepository subjectRepository;
    private final AcademicSessionRepository sessionRepository;
    private final ProgramRepository programRepository;

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
}