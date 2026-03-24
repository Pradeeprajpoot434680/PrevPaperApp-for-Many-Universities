package com.prevpaper.university.controller;

import com.prevpaper.comman.dto.ApiResponse;
import com.prevpaper.university.dtos.AssignRepRequest;
import com.prevpaper.university.dtos.UniversityRequest;
import com.prevpaper.university.entities.University;
import com.prevpaper.university.service.GlobalAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/global-admin")
@RequiredArgsConstructor
public class GlobalAdminController {

    private final GlobalAdminService globalAdminService;

    // Create a new university
    @PostMapping("/create-university")
    public ApiResponse<University> createUniversity(@RequestBody UniversityRequest request) {
        try {
            University university = globalAdminService.createUniversity(request);
            return ApiResponse.success("University created successfully", university);
        } catch (Exception e) {
            return ApiResponse.error("Failed to create university: " + e.getMessage());
        }
    }

    // Assign a university representative
    @PostMapping("/assign-rep")
    public ApiResponse<String> assignUniversityRep(@RequestBody AssignRepRequest request,
                                                   @RequestHeader("X-User-Id") String adminIdStr) {
        try {
            UUID currentAdminId = UUID.fromString(adminIdStr);

            globalAdminService.assignUniversityRep(request, currentAdminId);

            // TODO send request to change roles in user service user_roles (userId,scopeId)

            return ApiResponse.success("Representative assigned successfully", null);
        } catch (Exception e) {
            return ApiResponse.error("Failed to assign representative: " + e.getMessage());
        }
    }
}