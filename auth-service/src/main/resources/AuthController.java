package com.prevpaper.auth.controllers;

import com.prevpaper.auth.config.JwtService;
import com.prevpaper.auth.dto.*;
import com.prevpaper.auth.entities.User;
import com.prevpaper.auth.repositories.UserRepository;
import com.prevpaper.auth.services.AuthService;
import com.prevpaper.comman.dto.ApiResponse;
import com.prevpaper.comman.dto.AuthResponse; // Corrected import for your payload context [cite: 422, 441]
import com.prevpaper.comman.enums.AccountStatus; // Corrected enum reference [cite: 409, 416]
import com.prevpaper.common.service.RedisService; // Standardized to common library package path

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/auth")
@Slf4j // MANDATORY: Enables the 'log' object compilation
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final RedisService redisService;
    private final UserRepository userRepository; // Added missing repository dependency
    private final RedisTemplate<String, Object> redisTemplate; // Added missing redisTemplate dependency

    // Single unified constructor for pristine constructor injection
    public AuthController(AuthService authService,
                          JwtService jwtService,
                          RedisService redisService,
                          UserRepository userRepository,
                          RedisTemplate<String, Object> redisTemplate) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.redisService = redisService;
        this.userRepository = userRepository;
        this.redisTemplate = redisTemplate;
    }

    @GetMapping("/hello")
    public String saHello() {
        return "hello ";
    }

    @PostMapping("/signup")
    public ApiResponse<Map<String, String>> registerUser(@RequestBody SignupRequest signupRequest, HttpServletRequest request) {
        return authService.registerUser(signupRequest, request); // [cite: 404]
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, String>> loginUser(@RequestBody LoginRequest loginRequest,
                                                      HttpServletRequest req,
                                                      HttpServletResponse response) {
        log.info("Login request received for user: {}", loginRequest.getEmail());
        ApiResponse<Map<String, String>> apiResponse = authService.loginUser(loginRequest, req, response); // [cite: 419]

        // If sign-in is successful, proactively populate the Redis validation cache
        if (apiResponse != null && apiResponse.isSuccess() && apiResponse.getData() != null) {
            String accessToken = apiResponse.getData().get("accessToken");

            if (accessToken != null) {
                String redisTokenKey = "token:val:" + accessToken;

                try {
                    // 1. Extract username/email from token using your JwtService
                    String email = jwtService.extractUsername(accessToken);

                    // 2. Fetch the user details from repository to build the dynamic AuthResponse payload
                    userRepository.findByEmail(email).ifPresent(user -> {
                        AuthResponse cachedContext = new AuthResponse(
                                true, // isValid [cite: 422]
                                user.getAccountStatus() == AccountStatus.ACTIVE, // isVerified [cite: 409, 416]
                                user.getRoles().stream()
                                        .map(role -> role.getRoleName().name())
                                        .map(Object::toString)
                                        .toList(), // roles [cite: 422]
                                user.getUniversityId() != null ? user.getUniversityId().toString() : null, // universityId [cite: 441]
                                user.getUserId().toString(), // userId [cite: 422]
                                email, // email
                                user.getAssignedScopeId() != null ? user.getAssignedScopeId().toString() : null // scopeId
                        );

                        // 3. Proactively seed into Redis for 5 Minutes (300 seconds) to completely avoid Gateway Feign latency
                        redisService.set(redisTokenKey, cachedContext, 300L);

                        // 4. Track token inside User Set index for mass invalidation support on password alterations
                        String userTokensKey = "user:tokens:" + user.getUserId().toString();
                        redisTemplate.opsForSet().add(userTokensKey, redisTokenKey);

                        log.info("Proactively seeded token key inside Redis cache for fast Gateway loading.");
                    });
                } catch (Exception e) {
                    log.error("Failed to proactively seed login session into Redis: {}", e.getMessage());
                }
            }
        }

        return apiResponse;
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        // 1. Clear SecurityContext to immediately de-authenticate the current thread [cite: 434]
        SecurityContextHolder.clearContext();

        // 2. Clear the Refresh Token Cookie
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("Strict")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        // 3. CRITICAL SECURITY: Evict the token from the API Gateway's Redis cache [cite: 435]
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            String redisTokenKey = "token:val:" + token;

            try {
                redisService.delete(redisTokenKey);
                log.info("Successfully evicted token from Redis cache on logout.");
            } catch (Exception e) {
                log.error("Failed to evict token from Redis during logout: {}", e.getMessage());
            }
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Logged out successfully"
        ));
    }

    @PostMapping("/verify-otp")
    public ApiResponse<Map<String, String>> verifyOtp(
            @RequestBody VerifyOtpRequest request,
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        return authService.verifyOtp(request, httpRequest, httpResponse); // [cite: 412]
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Map<String, String>> forgotPassword(
            @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpRequest) {
        return authService.forgotPassword(request, httpRequest); // [cite: 425]
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse<Map<String, String>>> resendOTP(@RequestBody ResendOtpRequest request) {
        return ResponseEntity.ok(authService.resendOTP(request)); // [cite: 417]
    }

    @PostMapping("/reset-password")
    public ApiResponse<Map<String, String>> resetPassword(
            @RequestBody ResetPasswordRequest request,
            HttpServletRequest httpRequest) {

        // 1. Execute password change logic inside your DB repository [cite: 428]
        ApiResponse<Map<String, String>> response = authService.resetPassword(request, httpRequest);

        if (response.isSuccess()) {
            try {
                // 2. Extract the user's email from the request to locate their unique ID
                userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
                    String userTokensKey = "user:tokens:" + user.getUserId().toString();

                    // 3. Retrieve all cached token validation keys associated with this user ID
                    Set<Object> activeTokenKeys = redisTemplate.opsForSet().members(userTokensKey);

                    if (activeTokenKeys != null && !activeTokenKeys.isEmpty()) {
                        for (Object tokenKey : activeTokenKeys) {
                            // 4. Evict each individual validation token from Redis cache immediately [cite: 430]
                            redisService.delete(tokenKey.toString());
                        }
                    }

                    // 5. Delete the tracking index set itself
                    redisService.delete(userTokensKey);
                    log.info("Password changed successfully. Invalidated {} cached tokens for user safety.",
                            activeTokenKeys != null ? activeTokenKeys.size() : 0);
                });
            } catch (Exception e) {
                log.error("Failed to clear active sessions during password reset context execution: {}", e.getMessage());
            }
        }

        return response;
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Map<String, String>>> refresh(
            HttpServletRequest request,
            HttpServletResponse response) {
        try {
            ApiResponse<Map<String, String>> res = authService.handleRefresh(request, response);
            return ResponseEntity.ok(res);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(ApiResponse.error(e.getMessage()));
        }
    }
}