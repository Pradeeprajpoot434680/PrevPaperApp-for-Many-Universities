package com.prevpaper.auth.dto;

import com.prevpaper.comman.enums.TokenType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResendOtpRequest {
    private String recipient;
    private TokenType type;
}
