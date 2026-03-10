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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/university-rep")
@RequiredArgsConstructor
public class UniversityRepresentativeController {

    private final UniversityRepresentativeService universityRepService;

    @PostMapping("/department/create")
    public ResponseEntity<ApiResponse<Department>> createDepartment(@RequestBody DepartmentRequest request) {

        Department savedDept = universityRepService.createDepartment(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Department created successfully", savedDept, System.currentTimeMillis()));
    }

    @PostMapping("/department/assign-rep")
    public ResponseEntity<ApiResponse<Void>> assignDeptRep(@RequestBody AssignRepRequest request) {
        // TODO: Extract the University Rep's userId from JWT to use as 'assignedBy'
        UUID currentAdminId = UUID.randomUUID();

        universityRepService.assignDepartmentRep(request, currentAdminId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Department Representative assigned", null, System.currentTimeMillis()));
    }
}