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
import com.prevpaper.comman.config.SendNotification;
import com.prevpaper.comman.dto.ApiResponse;
import com.prevpaper.comman.dto.CommonNotificationRequest;
import com.prevpaper.comman.enums.*;

// CORRECTED IMPORTS: Use jakarta instead of javax
import com.prevpaper.comman.exception.*;
import com.prevpaper.comman.producer.NotificationProducer;
import com.twilio.Twilio;
import jakarta.annotation.PostConstruct;
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
    private  final  SendNotification sendNotification;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;
    @Value("${twilio.account_sid}")
    private String accountSid;

    @Value("${twilio.auth_token}")
    private String authToken;

    @Value("${twilio.phone_number}")
    private String fromNumber;

    @Value("${twilio.verify_service_sid}")
    private String verifyServiceSid;

    @PostConstruct
    public void initTwilio() {
        Twilio.init(accountSid, authToken);
    }
    // Signup Method
    @Override
    @Transactional
    public ApiResponse<Map<String, String>> registerUser(SignupRequest request, HttpServletRequest req) {

        // 1️⃣ Validate input
        if ((request.getEmail() == null || request.getEmail().isBlank()) &&
                (request.getPhoneNumber() == null || request.getPhoneNumber().isBlank())) {
            return new ApiResponse<>(false, "Email or Phone Number is required", null, System.currentTimeMillis());
        }

        if (request.getEmail() != null && userRepository.existsByEmail(request.getEmail())) {
            return new ApiResponse<>(false, "Email already exists", null, System.currentTimeMillis());
        }

        String cleanPhone = null;
        if (request.getPhoneNumber() != null) {
            cleanPhone = request.getPhoneNumber().replaceAll("\\s+", "");
            if (userRepository.existsByPhoneNumber(cleanPhone)) {
                return new ApiResponse<>(false, "Phone number already exists", null, System.currentTimeMillis());
            }
        }

        // 2️⃣ Assign default role
        Role studentRole = roleRepository.findByRoleName(UserRole.STUDENT)
                .orElseThrow(() -> new RoleExceptionHandler("Default Role not found"));

        User user = User.builder()
                .email(request.getEmail())
                .phoneNumber(cleanPhone)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .universityId(request.getUniversityId())
                .provider(AuthProvider.INTERNAL)
                .accountStatus(AccountStatus.PENDING)
                .roles(Set.of(studentRole))
                .build();

        userRepository.save(user);

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        NotificationType channelType;
        String recipient;
        boolean isEmail = false;

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            channelType = NotificationType.EMAIL;
            recipient = request.getEmail();
            isEmail = true;
        } else {
            channelType = NotificationType.SMS;
            recipient = cleanPhone;
        }

        String otpMessage;
        if (isEmail) {
            // ✅ Email: generate OTP, save in DB
            String otp = GererateOtp.getOTP();
            String hashedOtp = passwordEncoder.encode(otp);

            verificationTokenRepository.save(
                    VerificationToken.builder()
                            .token(hashedOtp)
                            .user(user)
                            .type(TokenType.EMAIL_VERIFY)
                            .expiryDate(LocalDateTime.now().plusMinutes(10))
                            .verified(false)
                            .build()
            );

            otpMessage = "Your verification OTP is " + otp + ". It expires in 10 minutes.";
        } else {
            otpMessage = "OTP sent via SMS";
        }

        // 7️⃣ Send notification via Kafka / Notification service
        CommonNotificationRequest notificationRequest = CommonNotificationRequest.builder()
                .userId(user.getUserId())
                .recipient(recipient)
                .title("OTP Verification")
                .message(otpMessage)
                .notificationTypes(List.of(channelType))
                .eventType(NotificationEventType.OTP_SENT)
                .build();

        sendNotification.sendOTP(notificationRequest);

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
    @Transactional
    public ApiResponse<Map<String, String>> verifyOtp(VerifyOtpRequest request, HttpServletRequest httpRequest) {
        String recipient = request.getRecipient().trim();
        String otp = request.getOtp().trim();
        TokenType type = request.getType(); // EMAIL_VERIFY or PASSWORD_RESET

        // 1️⃣ Identify User
        User user = userRepository.findByEmail(recipient)
                .or(() -> userRepository.findByPhoneNumber(recipient.replaceAll("\\s+", "")))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean isVerified = false;

        // 2️⃣ Logic Branching
        if (recipient.contains("@")) {
            // --- EMAIL VERIFICATION (Local DB Check) ---
            VerificationToken otpToken = verificationTokenRepository
                    .findByUserAndType(user, type)
                    .orElseThrow(() -> new InvalidOtpException(
                            type == TokenType.PASSWORD_RESET
                                    ? "User not verified for password reset"
                                    : "No OTP found for this email"
                    ));

            if (otpToken.getExpiryDate().isBefore(LocalDateTime.now())) {
                verificationTokenRepository.delete(otpToken);
                throw new InvalidOtpException(
                        type == TokenType.PASSWORD_RESET
                                ? "Password reset OTP expired"
                                : "Email OTP expired"
                );
            }

            if (!passwordEncoder.matches(otp, otpToken.getToken())) {
                throw new InvalidOtpException(
                        type == TokenType.PASSWORD_RESET
                                ? "Invalid Password Reset OTP"
                                : "Invalid Email OTP"
                );
            }

            isVerified = true;

            if (type == TokenType.PASSWORD_RESET) {
                // Just mark OTP as verified
                otpToken.setVerified(true);
                verificationTokenRepository.save(otpToken);
            } else {
                // EMAIL_VERIFY → activate user
                user.setAccountStatus(AccountStatus.ACTIVE);
                userRepository.save(user);
                verificationTokenRepository.delete(otpToken);
            }

        } else {
            // --- PHONE VERIFICATION (Twilio API Check) ---
            try {
                String formattedPhone = recipient.replaceAll("\\s+", "");
                if (!formattedPhone.startsWith("+")) formattedPhone = "+91" + formattedPhone;

                com.twilio.rest.verify.v2.service.VerificationCheck check =
                        com.twilio.rest.verify.v2.service.VerificationCheck.creator(verifyServiceSid)
                                .setTo(formattedPhone)
                                .setCode(otp)
                                .create();

                if (!"approved".equals(check.getStatus())) {
                    throw new InvalidOtpException(
                            type == TokenType.PASSWORD_RESET
                                    ? "Invalid or expired SMS OTP for password reset"
                                    : "Invalid or expired SMS OTP"
                    );
                }

                isVerified = true;

                if (type == TokenType.EMAIL_VERIFY) {
                    user.setAccountStatus(AccountStatus.ACTIVE);
                    userRepository.save(user);
                } else {
                    // Twilio OTP for password reset is automatically verified, nothing else to do
                }

            } catch (Exception e) {
                throw new InvalidOtpException("Twilio verification failed: " + e.getMessage());
            }
        }

        // 3️⃣ Return response
        Map<String, String> responseMap = new HashMap<>();
        if (isVerified && type == TokenType.EMAIL_VERIFY) {
            String accessToken = jwtService.generateAccessToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);
            responseMap.put("accessToken", accessToken);
            responseMap.put("refreshToken", refreshToken);
            return ApiResponse.success("Account activated successfully", responseMap);
        } else if (isVerified && type == TokenType.PASSWORD_RESET) {
            return ApiResponse.success("User verified for password reset", Map.of("verified", "true"));
        }

        throw new InvalidOtpException("Verification failed");
    }

    @Override
    @Transactional
    public ApiResponse<Map<String, String>> forgotPassword(ForgotPasswordRequest request, HttpServletRequest httpRequest) {
        String recipient = request.getRecipient().trim();
        System.out.println(recipient);

        User user = userRepository.findByEmail(recipient)
                .or(() -> userRepository.findByPhoneNumber(recipient.replaceAll("\\s+", "")))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        System.out.println(user.getUsername());

        NotificationType channelType = recipient.contains("@") ? NotificationType.EMAIL : NotificationType.SMS;

        if (channelType == NotificationType.EMAIL) {
            verificationTokenRepository.deleteByUserAndType(user, TokenType.PASSWORD_RESET);
        }

        String otp = GererateOtp.getOTP();
        System.out.println("OTP :" + otp);

        CommonNotificationRequest notificationRequest = CommonNotificationRequest.builder()
                .userId(user.getUserId())
                .recipient(recipient)
                .title("Forgot Password")
                .message("Your OTP for password reset is " + otp + ". It expires in 10 minutes.")
                .notificationTypes(List.of(channelType))
                .eventType(NotificationEventType.OTP_SENT)
                .build();

        System.out.println("Starting sending...");
        sendNotification.sendOTP(notificationRequest);
        System.out.println("Sent");

        if (channelType == NotificationType.EMAIL) {
            verificationTokenRepository.save(
                    VerificationToken.builder()
                            .token(passwordEncoder.encode(otp))
                            .user(user)
                            .type(TokenType.PASSWORD_RESET)
                            .verified(false)
                            .expiryDate(LocalDateTime.now().plusMinutes(10))
                            .build()
            );
        }

        System.out.println("Saved in Verification Repo");

        return ApiResponse.success(
                "OTP sent successfully",
                Map.of("sendOTP", "true")
        );
    }



    @Override
    @Transactional
    public ApiResponse<Map<String, String>> resetPassword(
            ResetPasswordRequest request,
            HttpServletRequest httpRequest) {

        // 1️⃣ Validate input
        if(request.getRecipient() == null || request.getRecipient().isBlank() ||
                request.getNewPassword() == null || request.getNewPassword().isBlank() ||
                request.getConfirmPassword() == null || request.getConfirmPassword().isBlank()) {
            throw new EmptyInputBoxException("All fields are required");
        }

        if(!request.getNewPassword().equals(request.getConfirmPassword())){
            throw new WrongCredentialsException("Passwords do not match");
        }

        String recipient = request.getRecipient().trim();

        // 2️⃣ Find the user
        User user = userRepository.findByEmail(recipient)
                .or(() -> userRepository.findByPhoneNumber(recipient.replaceAll("\\s+", "")))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean canReset = verificationTokenRepository.existsByUserAndTypeAndVerified(user, TokenType.PASSWORD_RESET, true);
        if (!canReset) {
            throw new InvalidOtpException("User not verified for password reset");
        }

        // 4️⃣ Update password
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // 5️⃣ Clean up OTP token
        verificationTokenRepository.deleteByUserAndType(user, TokenType.PASSWORD_RESET);

        return ApiResponse.success(
                "Password reset successfully",
                Map.of("passwordReset", "true")
        );
    }
}