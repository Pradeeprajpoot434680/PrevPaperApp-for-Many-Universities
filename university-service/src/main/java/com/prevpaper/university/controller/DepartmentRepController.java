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
@RequestMapping("/api/v1/department-rep")
@RequiredArgsConstructor
public class DepartmentRepController {

    private final DepartmentRepService departmentRepService;

    @PostMapping("/program/create")
    public ResponseEntity<ApiResponse<Program>> createProgram(@RequestBody ProgramRequest request) {

        Program program = Program.builder()
                .name(request.getName())
                .department(Department.builder()
                        .id(request.getDepartmentId())
                        .build())
                .build();

        Program savedProgram = departmentRepService.createProgram(program);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Program created successfully",
                        savedProgram, System.currentTimeMillis())
        );
    }

    @PostMapping("/program/assign-rep")
    public ResponseEntity<ApiResponse<Void>> assignProgramRep(@RequestBody AssignRepRequest request) {

        // TODO: Extract Department Rep ID from JWT
        UUID currentAdminId = UUID.randomUUID();

        departmentRepService.assignProgramRep(request, currentAdminId);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Program Representative assigned",
                        null, System.currentTimeMillis())
        );
    }
}