package com.prevpaper.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank
    private String identifier; // This can be email OR phone number

    @NotBlank
    private String password;
}
