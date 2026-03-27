package com.prevpaper.auth.controllers;

import com.prevpaper.auth.entities.User;
import com.prevpaper.auth.repositories.UserRepository;
import com.prevpaper.comman.dto.UserInternalInfoDTO;
import com.prevpaper.comman.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth/internal")
public class InternalAuthController {

    private final UserRepository userRepository;

    public InternalAuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
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
}
