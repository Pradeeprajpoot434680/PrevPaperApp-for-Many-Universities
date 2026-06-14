package com.prevpaper.auth.services;

import com.prevpaper.auth.dto.*;
import com.prevpaper.comman.dto.ApiResponse;
import com.prevpaper.comman.dto.AuthResponse; // Dynamic mapping claims shared structure
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Map;

public interface AuthService {
    ApiResponse<Map<String, String>> registerUser(SignupRequest request, HttpServletRequest req);

    ApiResponse<Map<String, String>> loginUser(LoginRequest loginRequest,
                                               HttpServletRequest req,
                                               HttpServletResponse response);

    ApiResponse<Map<String, String>> verifyOtp(VerifyOtpRequest request,
                                               HttpServletRequest httpRequest,
                                               HttpServletResponse httpResponse);

    ApiResponse<Map<String, String>> resetPassword(ResetPasswordRequest request,
                                                   HttpServletRequest httpRequest);

    ApiResponse<Map<String, String>> handleRefresh(HttpServletRequest request, HttpServletResponse response);

    ApiResponse<Map<String, String>> forgotPassword(ForgotPasswordRequest request, HttpServletRequest httpRequest);

    ApiResponse<Map<String, String>> resendOTP(ResendOtpRequest request);

    // Low-Level Design Fix: Abstract function to retrieve token mapping context
    AuthResponse getAuthResponseByIdentifier(String identifier);
}