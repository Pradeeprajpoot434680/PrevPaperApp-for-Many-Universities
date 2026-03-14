package com.prevpaper.auth.dto;

import com.prevpaper.comman.enums.TokenType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VerifyOtpRequest {
    private String recipient;
    private String otp;
    private TokenType type;
}
