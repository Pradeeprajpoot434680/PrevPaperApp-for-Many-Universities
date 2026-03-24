package com.prevpaper.university.controller;

import com.prevpaper.comman.dto.ApiResponse;
import com.prevpaper.university.dtos.AssignRepRequest;
import com.prevpaper.university.dtos.SessionRequest;
import com.prevpaper.university.entities.AcademicSession;
import com.prevpaper.university.entities.Program;
import com.prevpaper.university.service.ProgramRepService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/program-rep/{programId}")
@RequiredArgsConstructor
public class ProgramRepController {

    private final ProgramRepService programRepService;

    @PostMapping("/create-session")
    public ResponseEntity<ApiResponse<AcademicSession>> createSession(@RequestBody SessionRequest request,
                                                                      @PathVariable UUID programId) {



        AcademicSession savedSession = programRepService.createSession(programId,request);

        return ResponseEntity.ok(
                new ApiResponse<>(true,
                        "Session created successfully",
                        savedSession,
                        System.currentTimeMillis())
        );
    }

    @PostMapping("/assign-rep")
    public ResponseEntity<ApiResponse<Void>> assignSessionRep(@RequestBody AssignRepRequest request,
                                                              @RequestHeader("X-User-Id") String adminIdStr) {
        UUID currentAdminId = UUID.fromString(adminIdStr);

        programRepService.assignSessionRep(request, currentAdminId);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Session Representative assigned",
                        null, System.currentTimeMillis())
        );
    }
}