package com.prevpaper.auth.controllers;

import com.prevpaper.auth.config.JwtService;
import com.prevpaper.auth.entities.User;
import com.prevpaper.auth.repositories.UserRepository;
import com.prevpaper.comman.dto.AuthResponse;
import com.prevpaper.comman.enums.AccountStatus;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class ValidateToken {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @GetMapping("/validate")
    public AuthResponse validate(@RequestParam("token") String token) {
        try {
            String email = jwtService.extractUsername(token);

            return userRepository.findByEmail(email)
                    .map(user -> new AuthResponse(
                            !jwtService.isTokenExpired(token), // 1. isValid
                            user.getAccountStatus() == AccountStatus.ACTIVE, // 2. isVerified
                            user.getRoles().stream()
                                    .map(role -> role.getRoleName().name())
                                    .map(Object::toString) // Ensures List<String>
                                    .toList(), // 3. roles
                            user.getUniversityId() != null ? user.getUniversityId().toString() : null, // 4. universityId
                            user.getUserId().toString(), // 5. userId
                            email, // 6. email
                            user.getAssignedScopeId() != null ? user.getAssignedScopeId().toString() : null // 7. scopeId
                    ))
                    // FIX: Added a 7th argument (null) for scopeId
                    .orElse(new AuthResponse(false, false, List.of(), null, null, null, null));

        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            // FIX: Added a 7th argument (null) for scopeId
            return new AuthResponse(false, false, List.of(), null, null, null, null);
        }
    }
}