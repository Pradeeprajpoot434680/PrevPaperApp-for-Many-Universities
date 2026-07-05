package com.prevpaper.university.controller;
import com.prevpaper.comman.dto.ApiResponse;
import com.prevpaper.comman.dto.PendingContentDTO;
import com.prevpaper.university.dtos.*;
import com.prevpaper.university.entities.AcademicSession;
import com.prevpaper.university.entities.Program;
import com.prevpaper.university.entities.Semester;
import com.prevpaper.university.entities.Subject;
import com.prevpaper.university.service.ProgramRepService;
import com.prevpaper.university.service.SessionRepService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
    public ResponseEntity<ApiResponse<SubjectResponseDTO>> addSubject(
            @PathVariable UUID sessionId,
            @PathVariable UUID semesterId,
            @RequestHeader("X-Scope-Id") String xScopeId,
            @RequestBody SubjectRequest request) {

        validateScope(sessionId, xScopeId);

        SubjectResponseDTO subject = sessionRepService.addSubject(semesterId, request);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Subject added", subject, System.currentTimeMillis())
        );
    }


    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<SessionRepDashboardDTO>> getDashboard(
            @PathVariable UUID sessionId,
            @RequestHeader("X-Scope-Id") String xScopeId) {

        // Security check: sessionId must match JWT scope
        validateScope(sessionId, xScopeId);

        SessionRepDashboardDTO dashboard = sessionRepService.getSessionDashboard(sessionId);
        return ResponseEntity.ok(ApiResponse.success("Dashboard data fetched", dashboard));
    }

    @GetMapping("/semesters/{semesterId}/subjects") // Changed name to semesterId
    public ResponseEntity<ApiResponse<List<SubjectResourceDTO>>> getSemesterSubjects(
            @PathVariable UUID sessionId,
            @PathVariable UUID semesterId, // Changed type to UUID
            @RequestHeader("X-Scope-Id") UUID xScopeId) {

        validateScope(sessionId, xScopeId);

        List<SubjectResourceDTO> subjects = sessionRepService.getSubjectsBySemesterId(semesterId);
        return ResponseEntity.ok(ApiResponse.success("Semester subjects fetched", subjects));
    }






    // 1. Fetch all pending contents for this session
    @GetMapping("/pending-content")
    public ResponseEntity<ApiResponse<List<PendingContentDTO>>> getMyPendingContent(
            @PathVariable UUID sessionId,
            @RequestHeader("X-Scope-Id") UUID xScopeId) {

        // Ensure the Rep is only looking at their assigned session
        validateScope(sessionId, xScopeId);

        List<PendingContentDTO> list = sessionRepService.getPendingContentBySession(sessionId);
        return ResponseEntity.ok(ApiResponse.success("Pending content for your session fetched", list));
    }

    /**
     * Update content status (Verify or Reject).
     * The Content Service will handle the user notification once updated.
     */
    @PatchMapping("/content/{contentId}/status")
    public ResponseEntity<ApiResponse<Void>> updateContentStatus(
            @PathVariable UUID sessionId,
            @PathVariable UUID contentId,
            @RequestHeader("X-Scope-Id") UUID xScopeId,
            @RequestHeader("X-User-Id") UUID repId,
            @RequestBody VerifyContentRequest request) {

        validateScope(sessionId, xScopeId);

        sessionRepService.updateContentStatus(contentId, repId, request);

        String message = "Content successfully " + request.status().toLowerCase();
        return ResponseEntity.ok(ApiResponse.success(message, null));
    }

    /**
     * Hard delete content from the session archive.
     */
    @DeleteMapping("/content/{contentId}")
    public ResponseEntity<ApiResponse<Void>> deleteContent(
            @PathVariable UUID sessionId,
            @PathVariable UUID contentId,
            @RequestHeader("X-Scope-Id") UUID xScopeId) {

        validateScope(sessionId, xScopeId);

        sessionRepService.deleteContent(contentId);
        return ResponseEntity.ok(ApiResponse.success("Content deleted from session archive", null));
    }


    private void validateScope(UUID sessionId, UUID xScopeId) {
        // Correct way: compare UUID to UUID using .equals()
        if (xScopeId == null || !sessionId.equals(xScopeId)) {
            throw new RuntimeException("Access Denied: Scope mismatch for Session ID.");
        }
    }

}