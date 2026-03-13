package com.prevpaper.auth.services.Impl;

import com.prevpaper.auth.config.JwtService;
import com.prevpaper.auth.dto.*;
import com.prevpaper.auth.entities.Session;
import com.prevpaper.auth.entities.User;
import com.prevpaper.auth.entities.Role;
import com.prevpaper.auth.entities.VerificationToken;
import com.prevpaper.auth.repositories.SessionRepository;
import com.prevpaper.auth.repositories.UserRepository;
import com.prevpaper.auth.repositories.RoleRepository;
import com.prevpaper.auth.repositories.VerificationTokenRepository;
import com.prevpaper.auth.services.AuditService;
import com.prevpaper.auth.services.AuthService;
import com.prevpaper.auth.utils.GererateOtp;
import com.prevpaper.comman.dto.ApiResponse;
import com.prevpaper.comman.dto.CommonNotificationRequest;
import com.prevpaper.comman.enums.*;

// CORRECTED IMPORTS: Use jakarta instead of javax
import com.prevpaper.comman.exception.*;
import com.prevpaper.comman.producer.NotificationProducer;
import jakarta.servlet.http.Cookie;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtService jwtService;
    private final SessionRepository sessionRepository;
    private final AuditService auditService;
    private  final VerificationTokenRepository verificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationProducer producer;


    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    // Signup Method
    @Override
    @Transactional
    public ApiResponse<Map<String, String>> registerUser(SignupRequest request, HttpServletRequest req) {

        if ((request.getEmail() == null || request.getEmail().isBlank()) &&
                (request.getPhoneNumber() == null || request.getPhoneNumber().isBlank())) {
            return new ApiResponse<>(false, "Email or Phone Number is required", null, System.currentTimeMillis());
        }
        System.out.println("UPto here all things are good");
        if (request.getEmail() != null && userRepository.existsByEmail(request.getEmail())) {
            return new ApiResponse<>(false, "Email already exists", null, System.currentTimeMillis());
        }
        if (request.getPhoneNumber() != null && userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            return new ApiResponse<>(false, "Phone number already exists", null, System.currentTimeMillis());
        }
        System.out.println("UPto here all things are good1");

        Role studentRole = roleRepository.findByRoleName(UserRole.STUDENT)
                .orElseThrow(() -> new RoleExceptionHandler("Default Role not found"));

        User user = User.builder()
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .universityId(request.getUniversityId())
                .provider(AuthProvider.INTERNAL)
                .accountStatus(AccountStatus.PENDING)
                .roles(Set.of(studentRole))
                .build();

        userRepository.save(user);
        System.out.println("UPto here all things are good2");
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        String otp = GererateOtp.getOTP();
        String hashedOtp = passwordEncoder.encode(otp);
        System.out.println("UPto here all things are good3");
        verificationTokenRepository.save(
                VerificationToken.builder()
                        .token(hashedOtp)
                        .user(user)
                        .type(TokenType.EMAIL_VERIFY)
                        .expiryDate(LocalDateTime.now().plusMinutes(10))
                        .build()
        );
        System.out.println("UPto here all things are good4");
        CommonNotificationRequest notificationRequest = CommonNotificationRequest.builder()
                .userId(user.getUserId())
                .recipient(request.getEmail() != null ? request.getEmail() : request.getPhoneNumber())
                .title("OTP Verification")
                .message("Your verification OTP is " + otp + ". It expires in 10 minutes.")
                .notificationTypes(List.of(NotificationType.EMAIL)) // for channel
                .eventType(com.prevpaper.comman.enums.NotificationEventType.OTP_SENT)        // business event
                .build();

        System.out.println(notificationRequest);

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        producer.sendOtpNotification(user.getUserId().toString(), notificationRequest);
                    }
                }
        );

        sessionRepository.save(
                Session.builder()
                        .user(user)
                        .refreshToken(refreshToken)
                        .expiresAt(LocalDateTime.now().plusSeconds(refreshExpiration / 1000))
                        .isRevoked(false)
                        .build()
        );

        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", accessToken);
        tokens.put("refreshToken", refreshToken);

        auditService.logAction(user, "SIGNUP", user.getUserId().toString(), req);

        return new ApiResponse<>(true, "User registered successfully", tokens, System.currentTimeMillis());
    }
    // Login Method
    @Override
    public ApiResponse<Map<String, String>> loginUser(LoginRequest loginRequest,
                                                      HttpServletRequest req,
                                                      HttpServletResponse response) {
        String id = loginRequest.getIdentifier();
        if (id == null || id.isBlank()) {
            return new ApiResponse<>(false, "Email or Phone Number is required", null, System.currentTimeMillis());
        }

        // Find User
        User user = userRepository.findByEmail(id)
                .or(() -> userRepository.findByPhoneNumber(id))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Verify Password
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())) {
            return new ApiResponse<>(false, "Incorrect password", null, System.currentTimeMillis());
        }

        // Generate Tokens
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        // Save Session
        sessionRepository.save(
                Session.builder()
                        .user(user)
                        .refreshToken(refreshToken)
                        .expiresAt(LocalDateTime.now().plusSeconds(refreshExpiration / 1000))
                        .isRevoked(false)
                        .build()
        );

        // Set Refresh Token in Cookie
        Cookie cookie = new Cookie("refreshToken", refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // true if HTTPS
        cookie.setPath("/");
        cookie.setMaxAge((int) (refreshExpiration / 1000));
        response.addCookie(cookie);

        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", accessToken);

        // Audit log
        auditService.logAction(user, "LOGIN", user.getUserId().toString(), req);

        return new ApiResponse<>(true, "Login successful", tokens, System.currentTimeMillis());
    }

    @Override
    public ApiResponse<Map<String, String>> verifyOtp(
            VerifyOtpRequest request,
            HttpServletRequest httpRequest) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        VerificationToken otpToken = verificationTokenRepository.findByUserAndType(user, TokenType.EMAIL_VERIFY)
                .orElseThrow(() -> new InvalidOtpException("OTP not found"));




        // Check OTP match
        if (!otpToken.getToken().equals(request.getOtp())) {
            throw new InvalidOtpException("Invalid OTP");
        }

        // Check expiry
        if (otpToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            verificationTokenRepository.delete(otpToken); // delete expired OTP
            throw new InvalidOtpException("OTP expired");
        }
        // Activate account
        user.setAccountStatus(AccountStatus.ACTIVE);
        userRepository.save(user);

        otpToken.setVerified(true);
        verificationTokenRepository.save(otpToken);


        return ApiResponse.success(
                "OTP verified successfully",
                Map.of("verified", "true")
        );
    }



    @Override
    public ApiResponse<Map<String, String>> forgotPassword(ForgotPasswordRequest request, HttpServletRequest httpRequest) {

        if(request.getEmail() == null || request.getEmail().isBlank()){
            throw new EmptyInputBoxException("Email is Required");
        }

        String email = request.getEmail().trim();


        Optional<User> userOptional = userRepository.findByEmail(email);

        if(userOptional.isEmpty()){
            return ApiResponse.success(
                    "If the email exists, OTP has been sent",
                    Map.of("success","true")
            );
        }

        User user = userOptional.get();

        verificationTokenRepository.deleteByUserAndType(user, TokenType.PASSWORD_RESET);

        String otp = GererateOtp.getOTP();

        verificationTokenRepository.save(
                VerificationToken.builder()
                        .token(otp)
                        .user(user)
                        .type(TokenType.PASSWORD_RESET)
                        .expiryDate(LocalDateTime.now().plusMinutes(10)) // 10 min expiry
                        .build()
        );



        // send otp by notification service
        return ApiResponse.success(
                "OTP Sent successfully",
                Map.of("send OTP", "true")
        );


    }



    @Override
    public ApiResponse<Map<String, String>> resetPassword(
            ResetPasswordRequest request,
            HttpServletRequest httpRequest) {

        if(request.getEmail() == null || request.getEmail().isBlank() ||
                request.getNewPassword() == null || request.getNewPassword().isBlank() ||
                request.getConfirmPassword() == null || request.getConfirmPassword().isBlank()) {
            throw new EmptyInputBoxException("All fields are required");
        }

        if(!request.getNewPassword().equals(request.getConfirmPassword())){
            throw new WrongCredentialsException("Passwords do not match");
        }

        String email = request.getEmail().trim();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        VerificationToken otpToken = verificationTokenRepository
                .findByUserAndTypeAndVerified(user, TokenType.PASSWORD_RESET, true)
                .orElseThrow(() -> new InvalidOtpException("OTP not verified or expired"));

        if (otpToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            verificationTokenRepository.delete(otpToken);
            throw new InvalidOtpException("OTP expired");
        }

        String hashedPassword = passwordEncoder.encode(request.getNewPassword());
        user.setPasswordHash(hashedPassword);
        userRepository.save(user);

        verificationTokenRepository.delete(otpToken);

        return ApiResponse.success(
                "Password reset successfully",
                Map.of("passwordReset", "true")
        );
    }
}