package com.prevpaper.university.controller;

import com.prevpaper.comman.dto.ApiResponse;
import com.prevpaper.comman.dto.StudentDTO;
import com.prevpaper.comman.dto.UserBatchRequest;
import com.prevpaper.comman.dto.UserDetailDTO;
import com.prevpaper.university.client.AuthClient;
import com.prevpaper.university.client.UserServiceClient;
import com.prevpaper.university.dtos.AssignRepRequest;
import com.prevpaper.university.dtos.ProgramRequest;
import com.prevpaper.university.dtos.RepresentativeDetailsDTO;
import com.prevpaper.university.entities.Program;
import com.prevpaper.university.entities.Department;
import com.prevpaper.university.service.DepartmentRepService;
import com.prevpaper.university.service.RepresentativeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/api/v1/department-rep/{departmentId}") // Scoped Mapping
@RequiredArgsConstructor
public class DepartmentRepController {

    private final DepartmentRepService departmentRepService;
    private  final RepresentativeService representativeService;
    private final UserServiceClient userServiceClient;
    private final AuthClient authClient;

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
    @GetMapping("/students")
    public List<StudentDTO> getEnrichedStudentsByDept(@PathVariable  UUID departmentId) {
        // 1. Fetch basic student profiles from User-Service
        List<StudentDTO> students = userServiceClient.getStudentsByDept(departmentId);

        if (students.isEmpty()) return Collections.emptyList();

        // 2. Collect all User IDs
        List<UUID> userIds = students.stream()
                .map(StudentDTO::authUserId)
                .toList();

        // 3. Batch fetch emails from Auth-Service
        // (Using the getUserDetailsBatch we set up earlier)
        UserBatchRequest batchRequest = new UserBatchRequest(userIds);
        List<UserDetailDTO> authDetails = authClient.getUserDetailsBatch(batchRequest);

        // 4. Map emails by UserID for O(1) lookup
        Map<UUID, String> emailMap = authDetails.stream()
                .collect(Collectors.toMap(UserDetailDTO::userId, UserDetailDTO::email));

        // 5. Merge email into the DTOs
        return students.stream().map(s -> new StudentDTO(
                s.authUserId(),
                s.fullName(),
                emailMap.getOrDefault(s.authUserId(), "N/A") // Enrich with email
        )).toList();
    }

    @GetMapping("/program-reps")
    public ResponseEntity<ApiResponse<List<RepresentativeDetailsDTO>>> getMyProgramReps(
            @PathVariable UUID departmentId) {

        List<RepresentativeDetailsDTO> reps = representativeService.getProgramRepsByDept(departmentId);

        return ResponseEntity.ok(ApiResponse.success("Program representatives fetched", reps));
    }

}