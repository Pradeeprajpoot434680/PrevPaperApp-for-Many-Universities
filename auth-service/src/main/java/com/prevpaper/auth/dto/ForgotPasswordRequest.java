package com.prevpaper.auth.dto;

import lombok.Data;

@Data
public class ForgotPasswordRequest {
    private String recipient;
}
