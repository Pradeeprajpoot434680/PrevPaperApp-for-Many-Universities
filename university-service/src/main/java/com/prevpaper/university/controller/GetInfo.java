package com.prevpaper.university.controller;

import com.prevpaper.comman.dto.ApiResponse;
import com.prevpaper.university.entities.Department;
import com.prevpaper.university.entities.Program;
import com.prevpaper.university.entities.University;
import com.prevpaper.university.repository.DepartmentRepository;
import com.prevpaper.university.repository.ProgramRepository;
import com.prevpaper.university.repository.UniversityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class GetInfo {

    private final DepartmentRepository departmentRepository;
    private final UniversityRepository universityRepository;
    private final ProgramRepository programRepository;
    /**
     * Endpoint: GET /api/v1/department/{id}
     * Returns department metadata (including name and id) for frontend consumption.
     */
    @GetMapping("/department/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDepartmentById(@PathVariable("id") String id) {
        log.info("Fetching department metadata for ID: {}", id);
        try {
            UUID deptUuid = UUID.fromString(id);
            Department department = departmentRepository.findById(deptUuid)
                    .orElseThrow(() -> new RuntimeException("Department not found with ID: " + id));

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("id", department.getId().toString());
            responseData.put("name", department.getName());
            if (department.getUniversity() != null) {
                responseData.put("universityId", department.getUniversity().getId().toString());
                responseData.put("universityName", department.getUniversity().getName());
            }

            return ResponseEntity.ok(
                    ApiResponse.success("Department metadata retrieved successfully", responseData)
            );
        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID format provided for department ID: {}", id);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid department ID format"));
        } catch (Exception e) {
            log.error("Error retrieving department: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve department details: " + e.getMessage()));
        }
    }

    /**
     * Endpoint: GET /api/v1/university/{id}
     * Returns metadata for a given university ID.
     */
    @GetMapping("/university/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUniversityById(@PathVariable("id") String id) {
        log.info("Fetching university metadata for ID: {}", id);
        try {
            UUID uniUuid = UUID.fromString(id);
            University university = universityRepository.findById(uniUuid)
                    .orElseThrow(() -> new RuntimeException("University not found with ID: " + id));

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("id", university.getId().toString());
            responseData.put("name", university.getName());
            responseData.put("code", university.getCode());
            responseData.put("logoUrl", university.getLogoUrl());

            return ResponseEntity.ok(
                    ApiResponse.success("University metadata retrieved successfully", responseData)
            );
        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID format provided for university ID: {}", id);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid university ID format"));
        } catch (Exception e) {
            log.error("Error retrieving university: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve university details: " + e.getMessage()));
        }
    }


    /**
     * Endpoint: GET /api/v1/program/{id}
     * Returns metadata for a given program ID (including name, duration, department name)
     */
    @GetMapping("/program/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProgramById(@PathVariable("id") String id) {
        log.info("Fetching program metadata for ID: {}", id);
        try {
            UUID progUuid = UUID.fromString(id);
            Program program = programRepository.findById(progUuid)
                    .orElseThrow(() -> new RuntimeException("Program not found with ID: " + id));

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("id", program.getId().toString());
            responseData.put("name", program.getName());
            responseData.put("durationYears", program.getDurationYears()); // or program.getDuration() depending on entity

            if (program.getDepartment() != null) {
                responseData.put("departmentId", program.getDepartment().getId().toString());
                responseData.put("departmentName", program.getDepartment().getName());

                if (program.getDepartment().getUniversity() != null) {
                    responseData.put("universityId", program.getDepartment().getUniversity().getId().toString());
                    responseData.put("universityName", program.getDepartment().getUniversity().getName());
                }
            }

            return ResponseEntity.ok(
                    ApiResponse.success("Program metadata retrieved successfully", responseData)
            );
        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID format provided for program ID: {}", id);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid program ID format"));
        } catch (Exception e) {
            log.error("Error retrieving program: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve program details: " + e.getMessage()));
        }
    }
}