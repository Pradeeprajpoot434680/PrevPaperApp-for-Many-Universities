package com.prevpaper.university.controller;


import com.prevpaper.comman.dto.ApiResponse;
import com.prevpaper.university.dtos.*;
import com.prevpaper.university.entities.Department;
import com.prevpaper.university.entities.ExamConfiguration;
import com.prevpaper.university.entities.University;
import com.prevpaper.university.repository.DepartmentRepository;
import com.prevpaper.university.service.DepartmentRepService;
import com.prevpaper.university.service.RepresentativeService;
import com.prevpaper.university.service.UniversityRepresentativeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
@RestController
@RequestMapping("/api/v1/university-rep/{universityId}")
public class UniversityRepresentativeController {

    private final UniversityRepresentativeService universityRepresentativeService;
    private final RepresentativeService representativeService;
    private final DepartmentRepService departmentRepService;

    public UniversityRepresentativeController(UniversityRepresentativeService universityRepresentativeService, RepresentativeService representativeService, DepartmentRepService departmentRepService) {
        this.universityRepresentativeService = universityRepresentativeService;
        this.representativeService = representativeService;
        this.departmentRepService = departmentRepService;
    }


    // CREATE DEPARTMENT
    @PostMapping("/create-department")
    public ResponseEntity<ApiResponse<DepartmentTinyDTO>> createDepartment(
            @PathVariable UUID universityId,
            @RequestBody DepartmentRequest request) {

        DepartmentTinyDTO savedDept =
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
            @PathVariable("universityId") UUID universityId) { // 🟢 FIXED: Explicit identifier declaration

        boolean exists = universityRepresentativeService.existsById(universityId);
        return ResponseEntity.ok(exists);
    }

    @GetMapping("/exam-formats")
    public ResponseEntity<ApiResponse<List<ExamConfigurationDTO>>> getExamTypes(@PathVariable UUID universityId){
        ApiResponse<List<ExamConfigurationDTO>> examTypes = universityRepresentativeService.getExamTypes(universityId);
        return ResponseEntity.ok(examTypes);
    }

    @PostMapping("/exam-formats")
    public ResponseEntity<ApiResponse<String>> configureExams(
            @PathVariable UUID universityId,
            @RequestBody ExamFormatRequest format) { // single object now

        ApiResponse<String> response = universityRepresentativeService.addExamFormat(universityId, format);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/department-rep")
    public ResponseEntity<ApiResponse<List<DepartmentRepResponse>>> getDepartmentRepresentatives(
            @PathVariable UUID universityId) {

        List<DepartmentRepResponse> deptRepsByUniversity =
                representativeService.getDeptRepsByUniversity(universityId);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Department representatives fetched successfully",
                        deptRepsByUniversity,
                        System.currentTimeMillis()
                )
        );
    }

    @GetMapping("/departments")
    public ResponseEntity<ApiResponse<List<DepartmentTinyDTO>>> getAllDepartments(@PathVariable UUID universityId){

        List<DepartmentTinyDTO> allDepts = departmentRepService.findDepartmentsByUniversityId(universityId);
        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Department fetched successfully",
                        allDepts,
                        System.currentTimeMillis()
                )
        );
    }


}