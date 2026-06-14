package com.prevpaper.auth.services.Impl;

import com.prevpaper.auth.client.UniversityClient;
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
import com.prevpaper.comman.dto.AuthResponse;
import com.prevpaper.comman.dto.CommonNotificationRequest;
import com.prevpaper.comman.enums.*;
import com.prevpaper.comman.exception.*;
import com.prevpaper.comman.producer.NotificationProducer;
import com.twilio.Twilio;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtService jwtService;
    private final SessionRepository sessionRepository;
    private final AuditService auditService;
    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationProducer producer;
    private final SendNotification sendNotification;
    private final UniversityClient universityClient;

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

    @Override
    @Transactional
    public ApiResponse<Map<String, String>> registerUser(SignupRequest request, HttpServletRequest req) {
        log.info("Signup request received: email={}, phone={}, universityId={}", request.getEmail(), request.getPhoneNumber(), request.getUniversityId());

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

        if (request.getUniversityId() == null) {
            return new ApiResponse<>(false, "University selection is mandatory.", null, System.currentTimeMillis());
        }

        try {
            Boolean exists = universityClient.checkUniversityExists(request.getUniversityId());
            if (exists == null || !exists) {
                return new ApiResponse<>(false, "Invalid University: This institution is not registered.", null, System.currentTimeMillis());
            }
        } catch (Exception e) {
            log.error("University validation failed: {}", e.getMessage());
            return new ApiResponse<>(false, "University validation failed. Please try again later.", null, System.currentTimeMillis());
        }

        Role studentRole = roleRepository.findByRoleName(UserRole.STUDENT)
                .orElseThrow(() -> new RoleExceptionHandler("Default Role not found"));

        User user = User.builder()
                .email(request.getEmail())
                .phoneNumber(cleanPhone)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .universityId(request.getUniversityId())
                .assignedScopeId(request.getUniversityId()) // Multi-Tenancy Fix: Seed scope mapping for Gateway tracking
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

        CommonNotificationRequest notificationRequest = CommonNotificationRequest.builder()
                .userId(user.getUserId())
                .recipient(recipient)
                .title("OTP Verification")
                .message(otpMessage)
                .notificationTypes(List.of(channelType))
                .eventType(NotificationEventType.OTP_SENT)
                .build();

        // Transaction Synchronization Fix: Delay Kafka push until database commit transaction completes
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendNotification.sendOTP(notificationRequest);
                    log.info("Verification OTP transaction event securely committed and pushed to Kafka.");
                }
            });
        } else {
            sendNotification.sendOTP(notificationRequest);
        }

        sessionRepository.save(
                Session.builder()
                        .user(user)
                        .refreshToken(refreshToken)
                        .expiresAt(LocalDateTime.now().plusSeconds(refreshExpiration / 1000))
                        .isRevoked(false)
                        .build()
        );

        Map<String, String> tokens = new HashMap<>();
        tokens.put("Success", "true");

        auditService.logAction(user, "SIGNUP", user.getUserId().toString(), req);
        return new ApiResponse<>(true, "User registered successfully", tokens, System.currentTimeMillis());
    }

    @Override
    public ApiResponse<Map<String, String>> loginUser(LoginRequest loginRequest, HttpServletRequest req, HttpServletResponse response) {
        String id = loginRequest.getIdentifier();
        log.info("Login request received for identifier: {}", id);

        String genericError = "Invalid email/phone or password";

        User user = userRepository.findByEmail(id)
                .or(() -> userRepository.findByPhoneNumber(id))
                .orElseThrow(() -> new ResourceNotFoundException("User not registered. Please sign up first."));

        if (user.getAccountStatus() == AccountStatus.BANNED) {
            return new ApiResponse<>(false, "Account is BANNED", null, System.currentTimeMillis());
        }

        if (user.getAccountStatus() == AccountStatus.PENDING) {
            return new ApiResponse<>(false, "Please verify your account first", null, System.currentTimeMillis());
        }

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())) {
            return new ApiResponse<>(false, genericError, null, System.currentTimeMillis());
        }

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        sessionRepository.save(
                Session.builder()
                        .user(user)
                        .refreshToken(refreshToken)
                        .expiresAt(LocalDateTime.now().plusSeconds(refreshExpiration / 1000))
                        .isRevoked(false)
                        .build()
        );

        addRefreshTokenCookie(response, refreshToken);

        Map<String, String> responseData = new HashMap<>();
        responseData.put("accessToken", accessToken);
        responseData.put("userId", String.valueOf(user.getUserId()));
        responseData.put("email", user.getEmail());
        responseData.put("fullName", user.getFullName());
        responseData.put("universityId", String.valueOf(user.getUniversityId()));

        String rolesString = user.getRoles().stream()
                .map(role -> role.getRoleName().name())
                .reduce((a, b) -> a + "," + b)
                .orElse("");
        responseData.put("roles", rolesString);

        auditService.logAction(user, "LOGIN", user.getUserId().toString(), req);
        return new ApiResponse<>(true, "Login successful", responseData, System.currentTimeMillis());
    }

    @Override
    @Transactional
    public ApiResponse<Map<String, String>> verifyOtp(VerifyOtpRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String recipient = request.getRecipient().trim();
        String otp = request.getOtp().trim();
        TokenType type = request.getType();

        User user = userRepository.findByEmail(recipient)
                .or(() -> userRepository.findByPhoneNumber(recipient.replaceAll("\\s+", "")))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean isVerified = false;

        if (recipient.contains("@")) {
            VerificationToken otpToken = verificationTokenRepository.findByUserAndType(user, type)
                    .orElseThrow(() -> new InvalidOtpException("No active token payload context matching conditions"));

            if (otpToken.getExpiryDate().isBefore(LocalDateTime.now())) {
                verificationTokenRepository.delete(otpToken);
                throw new InvalidOtpException("OTP has expired.");
            }

            if (!passwordEncoder.matches(otp, otpToken.getToken())) {
                throw new InvalidOtpException("Invalid Verification Code.");
            }

            isVerified = true;

            if (type == TokenType.PASSWORD_RESET) {
                otpToken.setVerified(true);
                verificationTokenRepository.save(otpToken);
            } else {
                user.setAccountStatus(AccountStatus.ACTIVE);
                userRepository.save(user);
                verificationTokenRepository.delete(otpToken);
            }
        } else {
            try {
                String formattedPhone = recipient.replaceAll("\\s+", "");
                if (!formattedPhone.startsWith("+")) formattedPhone = "+91" + formattedPhone;

                com.twilio.rest.verify.v2.service.VerificationCheck check =
                        com.twilio.rest.verify.v2.service.VerificationCheck.creator(verifyServiceSid)
                                .setTo(formattedPhone)
                                .setCode(otp)
                                .create();

                if (!"approved".equals(check.getStatus())) {
                    throw new InvalidOtpException("Invalid SMS code reference verification status.");
                }

                isVerified = true;

                if (type == TokenType.EMAIL_VERIFY) {
                    user.setAccountStatus(AccountStatus.ACTIVE);
                    userRepository.save(user);
                }
            } catch (Exception e) {
                throw new InvalidOtpException("Twilio tracking execution constraint failure: " + e.getMessage());
            }
        }

        Map<String, String> responseMap = new HashMap<>();
        if (isVerified && type == TokenType.EMAIL_VERIFY) {
            String accessToken = jwtService.generateAccessToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);
            addRefreshTokenCookie(httpResponse, refreshToken);

            responseMap.put("accessToken", accessToken);
            responseMap.put("userId", String.valueOf(user.getUserId()));
            responseMap.put("email", user.getEmail());
            return ApiResponse.success("Account activated successfully", responseMap);
        } else if (isVerified && type == TokenType.PASSWORD_RESET) {
            return ApiResponse.success("User verified for password reset", Map.of("verified", "true"));
        }

        throw new InvalidOtpException("Verification pipeline resolution fault.");
    }

    @Override
    @Transactional
    public ApiResponse<Map<String, String>> forgotPassword(ForgotPasswordRequest request, HttpServletRequest httpRequest) {
        String recipient = request.getRecipient().trim();

        User user = userRepository.findByEmail(recipient)
                .or(() -> userRepository.findByPhoneNumber(recipient.replaceAll("\\s+", "")))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        NotificationType channelType = recipient.contains("@") ? NotificationType.EMAIL : NotificationType.SMS;

        if (channelType == NotificationType.EMAIL) {
            verificationTokenRepository.deleteByUserAndType(user, TokenType.PASSWORD_RESET);
        }

        String otp = GererateOtp.getOTP();

        CommonNotificationRequest notificationRequest = CommonNotificationRequest.builder()
                .userId(user.getUserId())
                .recipient(recipient)
                .title("Forgot Password")
                .message("Your OTP for password reset is " + otp + ". It expires in 10 minutes.")
                .notificationTypes(List.of(channelType))
                .eventType(NotificationEventType.OTP_SENT)
                .build();

        sendNotification.sendOTP(notificationRequest);

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

        return ApiResponse.success("OTP sent successfully", Map.of("sendOTP", "true"));
    }

    @Override
    @Transactional
    public ApiResponse<Map<String, String>> resetPassword(ResetPasswordRequest request, HttpServletRequest httpRequest) {
        if (request.getRecipient() == null || request.getRecipient().isBlank() ||
                request.getNewPassword() == null || request.getNewPassword().isBlank()) {
            throw new EmptyInputBoxException("All fields are required");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new WrongCredentialsException("Passwords do not match");
        }

        String recipient = request.getRecipient().trim();

        User user = userRepository.findByEmail(recipient)
                .or(() -> userRepository.findByPhoneNumber(recipient.replaceAll("\\s+", "")))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean canReset = verificationTokenRepository.existsByUserAndTypeAndVerified(user, TokenType.PASSWORD_RESET, true);
        if (!canReset) {
            throw new InvalidOtpException("User not verified for password reset");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        verificationTokenRepository.deleteByUserAndType(user, TokenType.PASSWORD_RESET);
        return ApiResponse.success("Password reset successfully", Map.of("passwordReset", "true"));
    }

    @Override
    public ApiResponse<Map<String, String>> handleRefresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = null;
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("refreshToken".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                }
            }
        }

        if (refreshToken == null || jwtService.isTokenExpired(refreshToken)) {
            throw new RuntimeException("Refresh token missing or expired");
        }

        String email = jwtService.extractUsername(refreshToken);
        User user = userRepository.findByEmail(email).orElseThrow();

        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);

        Cookie cookie = new Cookie("refreshToken", newRefreshToken);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        response.addCookie(cookie);

        return ApiResponse.success("Token refreshed", Map.of("accessToken", newAccessToken));
    }

    @Override
    @Transactional
    public ApiResponse<Map<String, String>> resendOTP(ResendOtpRequest request) {
        String recipient = request.getRecipient().trim();
        TokenType type = request.getType();

        User user = userRepository.findByEmail(recipient)
                .or(() -> userRepository.findByPhoneNumber(recipient.replaceAll("\\s+", "")))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (type == TokenType.EMAIL_VERIFY && user.getAccountStatus() == AccountStatus.ACTIVE) {
            return new ApiResponse<>(false, "Account is already verified. Please login.", null, System.currentTimeMillis());
        }

        NotificationType channelType;
        String otpMessage;
        boolean isEmail = recipient.contains("@");

        if (isEmail) {
            channelType = NotificationType.EMAIL;
            verificationTokenRepository.deleteByUserAndType(user, type);

            String otp = GererateOtp.getOTP();
            String hashedOtp = passwordEncoder.encode(otp);

            verificationTokenRepository.save(
                    VerificationToken.builder()
                            .token(hashedOtp)
                            .user(user)
                            .type(type)
                            .expiryDate(LocalDateTime.now().plusMinutes(10))
                            .verified(false)
                            .build()
            );

            otpMessage = "Your new verification OTP is " + otp + ". It expires in 10 minutes.";
        } else {
            channelType = NotificationType.SMS;
            otpMessage = "A new OTP has been sent via SMS";
        }

        CommonNotificationRequest notificationRequest = CommonNotificationRequest.builder()
                .userId(user.getUserId())
                .recipient(recipient)
                .title(type == TokenType.PASSWORD_RESET ? "Password Reset" : "Account Verification")
                .message(otpMessage)
                .notificationTypes(List.of(channelType))
                .eventType(NotificationEventType.OTP_SENT)
                .build();

        sendNotification.sendOTP(notificationRequest);

        return new ApiResponse<>(true, "Verification code resent successfully!", Map.of("recipient", recipient), System.currentTimeMillis());
    }

    // Abstraction Fix: Implementation processing user checks dynamically by multiple tenant identifiers
    @Override
    public AuthResponse getAuthResponseByIdentifier(String identifier) {
        String cleanId = identifier.trim();
        return userRepository.findByEmail(cleanId)
                .or(() -> userRepository.findByPhoneNumber(cleanId.replaceAll("\\s+", "")))
                .map(user -> new AuthResponse(
                        true,
                        user.getAccountStatus() == AccountStatus.ACTIVE,
                        user.getRoles().stream().map(role -> role.getRoleName().name()).toList(),
                        user.getUniversityId() != null ? user.getUniversityId().toString() : null,
                        user.getUserId().toString(),
                        user.getEmail(),
                        user.getAssignedScopeId() != null ? user.getAssignedScopeId().toString() : null
                )).orElse(null);
    }

    private void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie("refreshToken", refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // Enable to true in production under HTTPS layers
        cookie.setPath("/");
        cookie.setMaxAge((int) (refreshExpiration / 1000));
        response.addCookie(cookie);
    }
}