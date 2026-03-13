package com.prevpaper.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.UUID;

@Data
public class SignupRequest {

    private String email; // Removed @NotBlank for flexibility


    private String phoneNumber; // Added for flexible login

    @NotBlank
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank
    private String fullName;

    private UUID universityId; // Reference to external University Service
}