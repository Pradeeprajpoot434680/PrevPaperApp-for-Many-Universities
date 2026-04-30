package com.prevpaper.university.controller;

import com.prevpaper.comman.dto.ApiResponse;
import com.prevpaper.university.dtos.ExamConfigurationDTO;
import com.prevpaper.university.dtos.IdNameDTO;
import com.prevpaper.university.dtos.ProgramStructureDTO;
import com.prevpaper.university.service.UniversityDiscoveryService;
import com.prevpaper.university.service.UniversityRepresentativeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/get")
@RequiredArgsConstructor
public class UniversityDiscoveryController {

    private final UniversityDiscoveryService discoveryService;
    private final UniversityRepresentativeService universityRepresentativeService;

    @GetMapping("/universities/{universityId}/departments")
    public ResponseEntity<List<IdNameDTO>> getDepartments(@PathVariable UUID universityId) {
        return ResponseEntity.ok(discoveryService.getDepartments(universityId));
    }

    @GetMapping("/departments/{departmentId}/programs")
    public ResponseEntity<List<IdNameDTO>> getPrograms(@PathVariable UUID departmentId) {
        return ResponseEntity.ok(discoveryService.getPrograms(departmentId));
    }

    @GetMapping("/programs/{programId}/structure")
    public ResponseEntity<ProgramStructureDTO> getStructure(@PathVariable UUID programId) {
        return ResponseEntity.ok(discoveryService.getProgramStructure(programId));
    }

    @GetMapping("/semesters/{semesterId}/subjects")
    public ResponseEntity<List<IdNameDTO>> getSubjects(@PathVariable UUID semesterId) {
        return ResponseEntity.ok(discoveryService.getSubjects(semesterId));
    }

    @GetMapping("/universities/{universityId}/exams")
    public ResponseEntity<List<IdNameDTO>> getExams(@PathVariable UUID universityId) {
        return ResponseEntity.ok(discoveryService.getExamConfigs(universityId));
    }

    @GetMapping("/exam-formats/{universityId}")
    public ResponseEntity<ApiResponse<List<ExamConfigurationDTO>>> getExamTypes(@PathVariable UUID universityId){
        ApiResponse<List<ExamConfigurationDTO>> examTypes = universityRepresentativeService.getExamTypes(universityId);
        return ResponseEntity.ok(examTypes);
    }

}
