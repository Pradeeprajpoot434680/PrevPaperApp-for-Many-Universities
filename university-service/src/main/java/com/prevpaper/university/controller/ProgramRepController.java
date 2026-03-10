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
@RequestMapping("/api/v1/program-rep")
@RequiredArgsConstructor
public class ProgramRepController {

    private final ProgramRepService programRepService;

    @PostMapping("/session/create")
    public ResponseEntity<ApiResponse<AcademicSession>> createSession(@RequestBody SessionRequest request) {

//        AcademicSession session = AcademicSession.builder()
//                .program(Program.builder()
//                        .id(request.getProgramId())
//                        .build())
//                .startYear(request.getStartYear())
//                .endYear(request.getEndYear())
//                .isActive(true) // or request.getIsActive()
//                .build();


        AcademicSession savedSession = programRepService.createSession(request);

        return ResponseEntity.ok(
                new ApiResponse<>(true,
                        "Session created successfully",
                        savedSession,
                        System.currentTimeMillis())
        );
    }

    @PostMapping("/session/assign-rep")
    public ResponseEntity<ApiResponse<Void>> assignSessionRep(@RequestBody AssignRepRequest request) {

        // TODO: Extract Program Rep ID from JWT
        UUID currentAdminId = UUID.randomUUID();

        programRepService.assignSessionRep(request, currentAdminId);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Session Representative assigned",
                        null, System.currentTimeMillis())
        );
    }
}