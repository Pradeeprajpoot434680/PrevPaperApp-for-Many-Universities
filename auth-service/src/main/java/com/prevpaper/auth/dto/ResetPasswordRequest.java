package com.prevpaper.auth.dto;

import lombok.Data;

@Data
public class ResetPasswordRequest {
    private String recipient;
    private String newPassword;
    private String confirmPassword;
}
