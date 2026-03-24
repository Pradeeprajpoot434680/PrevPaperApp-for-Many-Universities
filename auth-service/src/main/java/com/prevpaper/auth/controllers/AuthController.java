package com.prevpaper.auth.controllers;

import com.prevpaper.auth.config.JwtService;
import com.prevpaper.auth.dto.*;
import com.prevpaper.auth.services.AuthService;
import com.prevpaper.comman.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private  final AuthService authService;
    private final JwtService jwtService;
    public AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;

        this.jwtService = jwtService;
    }
    @GetMapping("/hello")
    String saHello(){
        return "hello ";
    }

    @PostMapping("/signup")
    ApiResponse<Map<String, String>> registerUser(@RequestBody SignupRequest signupRequest, HttpServletRequest request){
        return authService.registerUser(signupRequest,request);
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, String>> loginUser(@RequestBody LoginRequest loginRequest,
                                                      HttpServletRequest req,
                                                      HttpServletResponse response) {
        System.out.println(loginRequest); // debug to see deserialized object
        return authService.loginUser(loginRequest, req, response);
    }


    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        // 1. Clear SecurityContext to immediately "de-authenticate" the current request thread
        SecurityContextHolder.clearContext();

        // 2. (Optional) Get token from 'Authorization' header and add to your Blacklist service
        // String token = extractTokenFromHeader(request);
        // blacklistService.blacklist(token);

        return ResponseEntity.ok(Map.of("success", true, "message", "Logged out successfully"));
    }

    //verify-otp
    @PostMapping("/verify-otp")
    public ApiResponse<Map<String, String>> verifyOtp(
            @RequestBody VerifyOtpRequest request,
            HttpServletRequest httpRequest) {

         System.out.println(request);
        return authService.verifyOtp(request, httpRequest);
    }


    //forget-password
    @PostMapping("/forgot-password")
    public ApiResponse<Map<String, String>> forgotPassword(
            @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpRequest) {
        System.out.println("Reached controller");
        return authService.forgotPassword(request, httpRequest);
    }


    //reset-password
    @PostMapping("/reset-password")
    public ApiResponse<Map<String, String>> resetPassword(
            @RequestBody ResetPasswordRequest request,
            HttpServletRequest httpRequest) {

        return authService.resetPassword(request, httpRequest);
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Map<String, String>>> refresh(@RequestBody RefreshTokenRequest request) {
        try {
            Map<String, String> tokens = authService.refreshToken(request.getRefreshToken());
            return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", tokens));
        } catch (RuntimeException e) {
            // Handle expired token or user not found
            return ResponseEntity.status(401).body(ApiResponse.error(e.getMessage()));
        }
    }

}
