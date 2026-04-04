package com.prevpaper.university.controller;

import com.prevpaper.comman.dto.ApiResponse;
import com.prevpaper.comman.dto.StudentDTO;
import com.prevpaper.university.dtos.DepartmentDTO;
import com.prevpaper.university.dtos.ProgramDTO;
import com.prevpaper.university.dtos.UniversityResponseDTO;
import com.prevpaper.university.entities.University;
import com.prevpaper.university.service.RepresentativeService;
import com.prevpaper.university.service.UniversityRepresentativeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/get")

@RequiredArgsConstructor
public class GetResources {
    private final UniversityRepresentativeService universityRepresentativeService;
    private  final RepresentativeService representativeService;
    @GetMapping("/universities")
    public ResponseEntity<ApiResponse<List<UniversityResponseDTO>>> getAll() {

        List<UniversityResponseDTO> universities =
                universityRepresentativeService.findAll();

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


        return ResponseEntity.ok(ApiResponse.<List<DepartmentDTO>>builder()
                .message("Departments fetched successfully")
                .data(departments)
                .build());
    }

    @GetMapping("/programs/{departmentId}")
    public ResponseEntity<ApiResponse<List<ProgramDTO>>> getProgramsByDepartmentId(
            @PathVariable String departmentId) {

        UUID id = UUID.fromString(departmentId);

        List<ProgramDTO> programs = universityRepresentativeService
                .findProgramByDepartmentId(id);

        return ResponseEntity.ok(ApiResponse.<List<ProgramDTO>>builder()
                .message("Programs fetched successfully")
                .data(programs)
                .build());
    }

    @GetMapping("/{departmentId}/students")
    public ResponseEntity<ApiResponse<List<StudentDTO>>> getDeptStudents(@PathVariable("departmentId") UUID deptId) {
        List<StudentDTO> students = representativeService.getStudentsByDepartment(deptId);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Students fetched", students, System.currentTimeMillis())
        );
    }


}
