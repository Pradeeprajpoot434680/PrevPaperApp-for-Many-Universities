package com.prevpaper.university.controller;

import com.prevpaper.comman.dto.ApiResponse;
import com.prevpaper.comman.dto.StudentDTO;
import com.prevpaper.comman.dto.UserBatchRequest;
import com.prevpaper.comman.dto.UserDetailDTO;
import com.prevpaper.university.client.AuthClient;
import com.prevpaper.university.client.UserServiceClient;
import com.prevpaper.university.dtos.AssignRepRequest;
import com.prevpaper.university.dtos.SessionDashboardDTO;
import com.prevpaper.university.dtos.SessionRepDetailsDTO;
import com.prevpaper.university.dtos.SessionRequest;
import com.prevpaper.university.entities.AcademicSession;
import com.prevpaper.university.entities.Program;
import com.prevpaper.university.service.ProgramRepService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/program-rep/{programId}")
@RequiredArgsConstructor
public class ProgramRepController {

    private final ProgramRepService programRepService;
    private final UserServiceClient userServiceClient;
    private final AuthClient authClient;


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



    @GetMapping("/students")
    public ResponseEntity<ApiResponse<List<StudentDTO>>>getEnrichedStudentsByProgram(@PathVariable  UUID programId) {
        // 1. Fetch basic student profiles from User-Service
        List<StudentDTO> students = userServiceClient.getStudentsByProgram(programId);

        if (students.isEmpty())  {
            return ResponseEntity.ok(ApiResponse.success("Users",Collections.emptyList()));
        }

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
        List<StudentDTO> allStudents = students.stream().map(s -> new StudentDTO(
                s.authUserId(),
                s.fullName(),
                emailMap.getOrDefault(s.authUserId(), "N/A") // Enrich with email
        )).toList();

        return ResponseEntity.ok(ApiResponse.success("All User",allStudents));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<List<SessionDashboardDTO>>> getProgramDashboard(
            @PathVariable UUID programId) {

        List<SessionDashboardDTO> dashboard = programRepService.getProgramSessionsDashboard(programId);

        return ResponseEntity.ok(ApiResponse.success("Sessions fetched successfully", dashboard));
    }


    @GetMapping("/all-session-reps")
    public ResponseEntity<ApiResponse<List<SessionRepDetailsDTO>>> getAllSessionReps(
            @PathVariable UUID programId) {

        List<SessionRepDetailsDTO> reps = programRepService.getAllSessionRepsByProgram(programId);

        return ResponseEntity.ok(ApiResponse.success("Session representatives fetched", reps));
    }
}