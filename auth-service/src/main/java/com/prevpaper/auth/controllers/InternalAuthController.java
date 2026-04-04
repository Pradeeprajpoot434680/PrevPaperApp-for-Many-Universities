package com.prevpaper.auth.controllers;

import com.prevpaper.auth.dto.StudentsOfUniversityDTO;
import com.prevpaper.auth.entities.User;
import com.prevpaper.auth.repositories.UserRepository;
import com.prevpaper.auth.services.InternalSync;
import com.prevpaper.comman.dto.ApiResponse;
import com.prevpaper.comman.dto.UserBatchRequest;
import com.prevpaper.comman.dto.UserDetailDTO;
import com.prevpaper.comman.dto.UserInternalInfoDTO;
import com.prevpaper.comman.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth/internal")
public class InternalAuthController {

    private final UserRepository userRepository;
    private  final InternalSync internalSync;
    public InternalAuthController(UserRepository userRepository, InternalSync internalSync) {
        this.userRepository = userRepository;
        this.internalSync = internalSync;
    }

    @GetMapping("/user/{id}")
    public ResponseEntity<UserInternalInfoDTO> getInternalUserInfo(@PathVariable UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found in Auth DB"));

        return ResponseEntity.ok(UserInternalInfoDTO.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .build());
    }

    @PostMapping("/user/set-fullName")
    public void setFullName(UUID userId,String fullName){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found in Auth DB"));

        user.setFullName(fullName);
    }

    @GetMapping("/{universityId}/students")
    public ResponseEntity<ApiResponse<List<StudentsOfUniversityDTO>>> findAllStudents(
            @PathVariable String universityId) {

        // InternalSync calls the User-Service via Feign or RestTemplate
        List<StudentsOfUniversityDTO> allStudents = internalSync.findAllStudents(universityId);

        return ResponseEntity.ok(ApiResponse.success("Students fetched successfully", allStudents));
    }


    @PostMapping("/users/details-batch")
    public List<UserDetailDTO> getUserDetailsBatch(@RequestBody UserBatchRequest request) {
        // Fetch users in one single query for performance
        List<User> users = userRepository.findAllById(request.userIds());

        return users.stream()
                .map(u -> new UserDetailDTO(
                        u.getUserId(),
                        u.getEmail(),
                        u.getFullName() // Auth User entity has fullName
                ))
                .toList();
    }


}
