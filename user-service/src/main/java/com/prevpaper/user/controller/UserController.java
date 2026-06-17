package com.prevpaper.user.controller;

import com.prevpaper.comman.dto.ApiResponse;
import com.prevpaper.comman.dto.UserProfileDTO;
import com.prevpaper.user.dto.ProfileUpdateRequest;
import com.prevpaper.user.dto.UserRequest;
import com.prevpaper.user.dto.UserSaveResponseDTO;
import com.prevpaper.user.entity.Account;
import com.prevpaper.user.entity.UserPointTransaction;
import com.prevpaper.user.service.PointService;
import com.prevpaper.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final PointService pointService;
    private final UserService userService;

    @PostMapping("/{authUserId}/points")
    public ResponseEntity<Account> awardPoints(
            @PathVariable UUID authUserId,
            @RequestParam Integer amount,
            @RequestParam String reason,
            @RequestParam(required = false) String refId) {
        log.info("Award points request: authUserId={} amount={} reason={}", authUserId, amount, reason);
        return ResponseEntity.ok(pointService.addPoints(authUserId, amount, reason, refId));
    }

    @GetMapping("/me/profile")
    public ResponseEntity<ApiResponse<UserProfileDTO>> getMyFullProfile(
            @RequestHeader("X-User-Id") String authUserId) {
        log.info("Get profile request for authUserId={}", authUserId);
        UserProfileDTO profile = userService.getFullProfile(UUID.fromString(authUserId));
        return ResponseEntity.ok(ApiResponse.success("Profile fetched", profile));
    }

    @PatchMapping("/me/profile")
    public ResponseEntity<ApiResponse<UserProfileDTO>> updateProfile(
            @RequestHeader("X-User-Id") String authUserId,
            @RequestBody ProfileUpdateRequest request) {
        log.info("Update profile request for authUserId={}", authUserId);

        // 🟢 Controller now receives clean UserProfileDTO mapping (No LocalDateTime crashes!)
        UserProfileDTO updatedProfile = userService.updateProfile(UUID.fromString(authUserId), request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", updatedProfile));
    }

    @GetMapping("/me/points/history")
    public ResponseEntity<List<UserPointTransaction>> getMyPointHistory(
            @RequestHeader("X-User-Id") String authUserId) {
        log.info("Point history request for authUserId={}", authUserId);
        return ResponseEntity.ok(pointService.getHistory(UUID.fromString(authUserId)));
    }

    @PostMapping("/store")
    public ResponseEntity<Map<String, Object>> store(@RequestBody UserRequest userRequest) {
        log.info("Store user request received for firstName={} lastName={}", userRequest.getFirstName(), userRequest.getLastName());

        // 🟢 Controller receives secure creation response DTO
        UserSaveResponseDTO savedUserDto = userService.createUser(userRequest);
        return ResponseEntity.ok(Map.of(
                "message", "User created successfully",
                "data", savedUserDto
        ));
    }
}