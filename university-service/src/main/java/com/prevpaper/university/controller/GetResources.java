package com.prevpaper.university.controller;

import com.prevpaper.comman.dto.ApiResponse;
import com.prevpaper.comman.dto.StudentDTO;
import com.prevpaper.university.dtos.*;

import com.prevpaper.university.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/get")

@RequiredArgsConstructor
public class GetResources {
    private final UniversityRepresentativeService universityRepresentativeService;
    private  final RepresentativeService representativeService;
    private final GlobalAdminService globalAdminService;
    private  final GetResourcesService getResourcesService;
    private final UniversityDiscoveryService universityDiscoveryService;

    @GetMapping("/universities")
    public ResponseEntity<ApiResponse<List<UniversityResponseDTO>>> getAll() {
        // 🟢 FIXED: Calls the clean DTO cache method instead of mapping raw entities inside the serializer
        List<UniversityResponseDTO> universities = universityRepresentativeService.getCachedUniversities();

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Universities fetched successfully",
                        universities,
                        System.currentTimeMillis()
                )
        );
    }



    @GetMapping("/departments/{universityId}")
    public ResponseEntity<ApiResponse<List<DepartmentDTO>>> getAllDepartmentsByUniversityId(@PathVariable String universityId) {
        UUID id = UUID.fromString(universityId);

        List<DepartmentDTO> departments = universityRepresentativeService
                .findDepartmentByUniversityId(id);

        return ResponseEntity.ok(ApiResponse.success("Departments Fetched Successfully", departments));

    }

    @GetMapping("/programs/{departmentId}")
    public ResponseEntity<ApiResponse<List<ProgramDTO>>> getProgramsByDepartmentId(
            @PathVariable String departmentId) {

        UUID id = UUID.fromString(departmentId);

        List<ProgramDTO> programs = universityRepresentativeService
                .findProgramByDepartmentId(id);
        return ResponseEntity.ok(ApiResponse.success("Program Fetched Successfully", programs));

    }

    @GetMapping("/department/{departmentId}/students")
    public ResponseEntity<ApiResponse<List<StudentDTO>>> getDeptStudents(@PathVariable("departmentId") UUID deptId) {
        List<StudentDTO> students = representativeService.getStudentsByDepartment(deptId);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Students fetched of the Program", students, System.currentTimeMillis())
        );
    }

    @GetMapping("/program/{programId}/students")
    public ResponseEntity<ApiResponse<List<StudentDTO>>> getProgramStudents(@PathVariable("programId") UUID programId) {
        List<StudentDTO> students = representativeService.getStudentsByProgram(programId);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Students fetched of the Program", students, System.currentTimeMillis())
        );
    }

    @GetMapping("/all-university-reps")
    public ResponseEntity<ApiResponse<List<RepresentativeDetailsDTO>>> getAllUniversityReps() {
        List<RepresentativeDetailsDTO> reps = globalAdminService.getAllUniversityReps();
        return ResponseEntity.ok(ApiResponse.success("University Representatives fetched", reps));
    }

    @GetMapping("/dashboard/programs/{departmentId}")
    public ResponseEntity<ApiResponse<List<ProgramDashboardDTO>>> getProgramsForDashboard(
            @PathVariable UUID departmentId) {

        // This calls the complex logic we wrote earlier
        List<ProgramDashboardDTO> dashboardData = representativeService
                .getDepartmentProgramsDashboard(departmentId);

        return ResponseEntity.ok(ApiResponse.success("Dashboard data fetched", dashboardData));
    }


    @PostMapping("/academic-names")
    public ResponseEntity<ApiResponse<AcademicNamesResponse>> getAcademicNames(
            @RequestBody AcademicNamesRequest request) {

        // Fetch names from your repositories
       return ResponseEntity.ok(getResourcesService.getUniversityData(request));
    }


    @GetMapping("/university-info/{universityId}")
    public ResponseEntity<ApiResponse<List<UniversityTeamMemberDTO>>> getUniversityTeam(
            @PathVariable UUID universityId
    ) {
        List<UniversityTeamMemberDTO> team = universityDiscoveryService.getUniversityTeam(universityId);

        // Use the static success method you've used before
        return ResponseEntity.ok(ApiResponse.success("Team Fetched", team));
    }


    // 🟢 ADDED: Fetch all session batches using the program id
    // 🟢 ADDED: Fetch all session batches using the program id
    @GetMapping("/sessions/{programId}")
    public ResponseEntity<ApiResponse<List<SessionTinyDTO>>> getSessionsByProgramId(@PathVariable UUID programId) {
        // Calls the service layer to pull the sessions/batches attached to this specific program track
        List<SessionTinyDTO> sessions = representativeService.getSessionsByProgram(programId);
        return ResponseEntity.ok(ApiResponse.success("Sessions Fetched Successfully", sessions));
    }


    // get all semester using the program id
}
