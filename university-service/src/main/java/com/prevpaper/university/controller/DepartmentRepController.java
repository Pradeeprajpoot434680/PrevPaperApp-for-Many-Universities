package com.prevpaper.university.controller;

import com.prevpaper.comman.dto.ApiResponse;
import com.prevpaper.university.dtos.AssignRepRequest;
import com.prevpaper.university.dtos.ProgramRequest;
import com.prevpaper.university.entities.Program;
import com.prevpaper.university.entities.Department;
import com.prevpaper.university.service.DepartmentRepService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;


@RestController
@RequestMapping("/api/v1/department-rep/{departmentId}") // Scoped Mapping
@RequiredArgsConstructor
public class DepartmentRepController {

    private final DepartmentRepService departmentRepService;

    @PostMapping("/create-program")
    public ResponseEntity<ApiResponse<Program>> createProgram(
            @PathVariable UUID departmentId, // From URL for Gateway validation
            @RequestBody ProgramRequest request,
            @RequestHeader("X-User-Id") String adminIdStr) {

        UUID currentAdminId = UUID.fromString(adminIdStr);


        Program savedProgram = departmentRepService.createProgram(departmentId,request);

        return ResponseEntity.ok(
                ApiResponse.success("Program created successfully", savedProgram)
        );
    }

    @PostMapping("/assign-rep")
    public ResponseEntity<ApiResponse<Void>> assignProgramRep(
            @PathVariable UUID departmentId,
            @RequestBody AssignRepRequest request,
            @RequestHeader("X-User-Id") String adminIdStr) {

        UUID currentAdminId = UUID.fromString(adminIdStr);

        // Ensure the scopeId in the request matches the URL path
        // (Optional check, but good for data integrity)
        departmentRepService.assignProgramRep(request, currentAdminId);

        return ResponseEntity.ok(
                ApiResponse.success("Program Representative assigned", null)
        );
    }
}