package com.prevpaper.university.controller;


import com.prevpaper.comman.dto.ApiResponse;
import com.prevpaper.university.dtos.AssignRepRequest;
import com.prevpaper.university.dtos.DepartmentRequest;
import com.prevpaper.university.dtos.UniversityRequest;
import com.prevpaper.university.entities.Department;
import com.prevpaper.university.entities.University;
import com.prevpaper.university.repository.DepartmentRepository;
import com.prevpaper.university.service.UniversityRepresentativeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
@RestController
@RequestMapping("/api/v1/university-rep/{universityId}")
public class UniversityRepresentativeController {

    private final UniversityRepresentativeService universityRepresentativeService;

    public UniversityRepresentativeController(UniversityRepresentativeService universityRepresentativeService) {
        this.universityRepresentativeService = universityRepresentativeService;
    }


    // CREATE DEPARTMENT
    @PostMapping("/create-department")
    public ResponseEntity<ApiResponse<Department>> createDepartment(
            @PathVariable UUID universityId,
            @RequestBody DepartmentRequest request) {

        Department savedDept =
                universityRepresentativeService.createDepartment(universityId, request);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Department created successfully",
                        savedDept,
                        System.currentTimeMillis()
                )
        );
    }


    // ASSIGN DEPARTMENT REPRESENTATIVE
    @PostMapping("/assign-rep")
    public ResponseEntity<ApiResponse<Void>> assignDeptRep(
            @PathVariable UUID universityId,
            @RequestBody AssignRepRequest request,
            @RequestHeader("X-User-Id") String adminIdHeader) {

        UUID adminId = UUID.fromString(adminIdHeader);

        universityRepresentativeService.assignDepartmentRep(

                request,
                adminId
        );

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Department Representative assigned successfully",
                        null,
                        System.currentTimeMillis()
                )
        );
    }


    // CHECK UNIVERSITY EXISTS
    @GetMapping("/exists")
    public ResponseEntity<Boolean> checkUniversityExists(
            @PathVariable UUID universityId) {

        boolean exists = universityRepresentativeService.existsById(universityId);

        return ResponseEntity.ok(exists);
    }
}