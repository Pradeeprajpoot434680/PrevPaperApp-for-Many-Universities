package com.prevpaper.university.controller;
import com.prevpaper.comman.dto.ApiResponse;
import com.prevpaper.university.dtos.AssignRepRequest;
import com.prevpaper.university.dtos.SemesterRequest;
import com.prevpaper.university.dtos.SessionRequest;
import com.prevpaper.university.dtos.SubjectRequest;
import com.prevpaper.university.entities.AcademicSession;
import com.prevpaper.university.entities.Program;
import com.prevpaper.university.entities.Semester;
import com.prevpaper.university.entities.Subject;
import com.prevpaper.university.service.ProgramRepService;
import com.prevpaper.university.service.SessionRepService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/session-rep/{sessionId}")
@RequiredArgsConstructor
public class SessionRepController {

    private final SessionRepService sessionRepService;

    private void validateScope(UUID sessionId, String xScopeId) {
        if (xScopeId == null || !sessionId.toString().equals(xScopeId)) {
            throw new RuntimeException("Access Denied: You do not have permission for this session.");
        }
    }

    // Add semester into program
    @PostMapping("/programs/{programId}/semesters")
    public ResponseEntity<ApiResponse<Semester>> addSemester(
            @PathVariable UUID sessionId,
            @PathVariable UUID programId,
            @RequestHeader("X-Scope-Id") String xScopeId,
            @RequestBody SemesterRequest request) {

        validateScope(sessionId, xScopeId);

        Semester semester = sessionRepService.addSemester(programId, request);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Semester added", semester, System.currentTimeMillis())
        );
    }

    // Add subject into semester
    @PostMapping("/semesters/{semesterId}/subjects")
    public ResponseEntity<ApiResponse<Subject>> addSubject(
            @PathVariable UUID sessionId,
            @PathVariable UUID semesterId,
            @RequestHeader("X-Scope-Id") String xScopeId,
            @RequestBody SubjectRequest request) {

        validateScope(sessionId, xScopeId);

        Subject subject = sessionRepService.addSubject(semesterId, request);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Subject added", subject, System.currentTimeMillis())
        );
    }
}