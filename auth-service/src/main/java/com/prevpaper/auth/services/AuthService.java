package com.prevpaper.auth.services;

import com.prevpaper.auth.dto.LoginRequest;
import com.prevpaper.auth.dto.SignupRequest;
import com.prevpaper.auth.dto.VerifyOtpRequest;
import com.prevpaper.comman.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;

import java.util.Map;


public interface AuthService {
    ApiResponse<Map<String, String>> registerUser(SignupRequest request, HttpServletRequest req);

    ApiResponse<Map<String, String>> loginUser(LoginRequest loginRequest,
                                               HttpServletRequest req,
                                               HttpServletResponse response);
    ApiResponse<Map<String, String>> verifyOtp(VerifyOtpRequest request, HttpServletRequest httpRequest);
}