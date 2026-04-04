package com.prevpaper.user.controller;

import com.prevpaper.user.dto.UserRequest;
import com.prevpaper.user.entity.Account;
import com.prevpaper.user.entity.User;
import com.prevpaper.user.entity.UserPointTransaction;
import com.prevpaper.user.service.PointService;
import com.prevpaper.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final PointService pointService;
    private final UserService userService;
    // Award points (Internal or Admin use)
    @PostMapping("/{authUserId}/points")
    public ResponseEntity<Account> awardPoints(
            @PathVariable UUID authUserId,
            @RequestParam Integer amount,
            @RequestParam String reason,
            @RequestParam(required = false) String refId) {

        return ResponseEntity.ok(pointService.addPoints(authUserId, amount, reason, refId));
    }

    // Get Point History for the logged-in user
    @GetMapping("/me/points/history")
    public ResponseEntity<List<UserPointTransaction>> getMyPointHistory(
            @RequestHeader("X-User-Id") String authUserId) {

        return ResponseEntity.ok(pointService.getHistory(UUID.fromString(authUserId)));
    }
    @PostMapping("/store")
    public ResponseEntity<Map<String, Object>> store(@RequestBody UserRequest userRequest) {

        User savedUser = userService.createUser(userRequest);

        return ResponseEntity.ok(Map.of(
                "message", "User created successfully",
                "data", savedUser
        ));
    }

}