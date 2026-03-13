package com.prevpaper.auth.services;

import com.prevpaper.auth.dto.*;
import com.prevpaper.comman.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

public interface AuthService {
    ApiResponse<Map<String, String>> registerUser(SignupRequest request, HttpServletRequest req);

    ApiResponse<Map<String, String>> loginUser(LoginRequest loginRequest,
                                               HttpServletRequest req,
                                               HttpServletResponse response);
    ApiResponse<Map<String, String>> verifyOtp(VerifyOtpRequest request, HttpServletRequest httpRequest);


    ApiResponse<Map<String, String>> resetPassword(
            @RequestBody ResetPasswordRequest request,
            HttpServletRequest httpRequest) ;


    ApiResponse<Map<String,String>> forgotPassword(ForgotPasswordRequest request,HttpServletRequest httpRequest);
}