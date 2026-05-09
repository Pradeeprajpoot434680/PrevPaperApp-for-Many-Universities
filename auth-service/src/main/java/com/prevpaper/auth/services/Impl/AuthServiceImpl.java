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
    private  final VerificationTokenRepository verificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationProducer producer;
    private  final  SendNotification sendNotification;
    private  final UniversityClient universityClient;

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
        log.info("Signup request received: email={}, phone={}, universityId={}, path={}, clientIp={}",
                request.getEmail(), request.getPhoneNumber(), request.getUniversityId(), req.getRequestURI(), req.getRemoteAddr());

        if ((request.getEmail() == null || request.getEmail().isBlank()) &&
                (request.getPhoneNumber() == null || request.getPhoneNumber().isBlank())) {
            log.warn("Signup rejected: missing email and phone, universityId={}, path={}, clientIp={}",
                    request.getUniversityId(), req.getRequestURI(), req.getRemoteAddr());
            return new ApiResponse<>(false, "Email or Phone Number is required", null, System.currentTimeMillis());
        }

        if (request.getEmail() != null && userRepository.existsByEmail(request.getEmail())) {
            log.warn("Signup rejected: email already exists, email={}, universityId={}, path={}, clientIp={}",
                    request.getEmail(), request.getUniversityId(), req.getRequestURI(), req.getRemoteAddr());
            return new ApiResponse<>(false, "Email already exists", null, System.currentTimeMillis());
        }

        String cleanPhone = null;
        if (request.getPhoneNumber() != null) {
            cleanPhone = request.getPhoneNumber().replaceAll("\\s+", "");
            if (userRepository.existsByPhoneNumber(cleanPhone)) {
                log.warn("Signup rejected: phone already exists, phone={}, universityId={}, path={}, clientIp={}",
                        cleanPhone, request.getUniversityId(), req.getRequestURI(), req.getRemoteAddr());
                return new ApiResponse<>(false, "Phone number already exists", null, System.currentTimeMillis());
            }
        }

        if (request.getUniversityId() == null) {
            log.warn("Signup rejected: universityId missing, email={}, phone={}, path={}, clientIp={}",
                    request.getEmail(), cleanPhone, req.getRequestURI(), req.getRemoteAddr());
            return new ApiResponse<>(false, "University selection is mandatory.", null, System.currentTimeMillis());
        }

        // 2. REMOTE CALL via Feign
        try {
            log.info("Validating university for signup: email={}, phone={}, universityId={}",
                    request.getEmail(), cleanPhone, request.getUniversityId());
            Boolean exists = universityClient.checkUniversityExists(request.getUniversityId());
            if (exists == null || !exists) {
                log.warn("Signup rejected: university validation failed, email={}, phone={}, universityId={}",
                        request.getEmail(), cleanPhone, request.getUniversityId());
                return new ApiResponse<>(false, "Invalid University: This institution is not registered.", null, System.currentTimeMillis());
            }
        } catch (Exception e) {
            // Handle case where University Service is down
            log.error("Signup blocked: university validation service error, email={}, phone={}, universityId={}, error={}",
                    request.getEmail(), cleanPhone, request.getUniversityId(), e.getMessage(), e);
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
                .provider(AuthProvider.INTERNAL)
                .accountStatus(AccountStatus.PENDING)
                .roles(Set.of(studentRole))
                .build();

        userRepository.save(user);
        log.info("User created with pending status: userId={}, email={}, phone={}, universityId={}",
                user.getUserId(), user.getEmail(), user.getPhoneNumber(), user.getUniversityId());

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
            log.info("Email verification token stored for signup: userId={}, email={}, tokenType={}",
                    user.getUserId(), user.getEmail(), TokenType.EMAIL_VERIFY);
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
        log.info("Signup OTP notification requested: userId={}, recipient={}, channel={}, eventType={}",
                user.getUserId(), recipient, channelType, NotificationEventType.OTP_SENT);

        sessionRepository.save(
                Session.builder()
                        .user(user)
                        .refreshToken(refreshToken)
                        .expiresAt(LocalDateTime.now().plusSeconds(refreshExpiration / 1000))
                        .isRevoked(false)
                        .build()
        );
        log.info("Signup session created: userId={}, expiresInMs={}", user.getUserId(), refreshExpiration);

        Map<String, String> tokens = new HashMap<>();
        tokens.put("Success", "true");
//        tokens.put("refreshToken", refreshToken);

        auditService.logAction(user, "SIGNUP", user.getUserId().toString(), req);
        log.info("Signup completed: userId={}, email={}, phone={}, universityId={}, status={}",
                user.getUserId(), user.getEmail(), user.getPhoneNumber(), user.getUniversityId(), user.getAccountStatus());

        return new ApiResponse<>(true, "User registered successfully", tokens, System.currentTimeMillis());
    }
    // Login Method
    @Override
    public ApiResponse<Map<String, String>> loginUser(LoginRequest loginRequest,
                                                      HttpServletRequest req,
                                                      HttpServletResponse response) {
        String id = loginRequest.getIdentifier();
        log.info("Login request received: identifier={}, path={}, clientIp={}",
                id, req.getRequestURI(), req.getRemoteAddr());

        // 1. Generic Error Message to prevent enumeration
        String genericError = "Invalid email/phone or password";

        // 2. Find User
        Optional<User> userOpt = userRepository.findByEmail(id)
                .or(() -> userRepository.findByPhoneNumber(id));

        if (userOpt.isEmpty()) {
            // User not signed up
            // Optional: Add slight delay or dummy password hash check to prevent timing attacks
            log.warn("Login rejected: user not registered, identifier={}, path={}, clientIp={}",
                    id, req.getRequestURI(), req.getRemoteAddr());
            return new ApiResponse<>(false, "User not registered. Please sign up first.", null, System.currentTimeMillis());
        }


        User user = userOpt.get();

        // 3. Status Check (Existing)
        if (user.getAccountStatus() == AccountStatus.BANNED ) {
            log.warn("Login rejected: banned account, userId={}, email={}, phone={}, status={}",
                    user.getUserId(), user.getEmail(), user.getPhoneNumber(), user.getAccountStatus());
            return new ApiResponse<>(false, "Account is " + user.getAccountStatus(), null, System.currentTimeMillis());
        }

        // Special handling for PENDING (optional: allow login but redirect to OTP page)
        if (user.getAccountStatus() == AccountStatus.PENDING) {
            log.warn("Login rejected: account pending verification, userId={}, email={}, phone={}, status={}",
                    user.getUserId(), user.getEmail(), user.getPhoneNumber(), user.getAccountStatus());
            return new ApiResponse<>(false, "Please verify your account first", null, System.currentTimeMillis());
        }

        // 4. Password Check
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())) {
            // Update failed attempts here if you implement locking!
            log.warn("Login rejected: invalid password, userId={}, email={}, phone={}, path={}, clientIp={}",
                    user.getUserId(), user.getEmail(), user.getPhoneNumber(), req.getRequestURI(), req.getRemoteAddr());
            return new ApiResponse<>(false, genericError, null, System.currentTimeMillis());
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
        log.info("Login session created: userId={}, email={}, expiresInMs={}",
                user.getUserId(), user.getEmail(), refreshExpiration);

//        // Set Refresh Token in Cookie
//        Cookie cookie = new Cookie("refreshToken", refreshToken);
//        cookie.setHttpOnly(true);
//        cookie.setSecure(false); // true if HTTPS
//        cookie.setPath("/");
//        cookie.setMaxAge((int) (refreshExpiration / 1000));
//        response.addCookie(cookie);
        addRefreshTokenCookie(response,refreshToken);
        System.out.println("Refresh Token => "+  refreshToken );
        Map<String, String> responseData = new HashMap<>();
        responseData.put("accessToken", accessToken);
        responseData.put("userId", String.valueOf(user.getUserId()));
        responseData.put("email", user.getEmail());
        responseData.put("fullName", user.getFullName());
        responseData.put("universityId", String.valueOf(user.getUniversityId()));

        // Convert Roles Set to a comma-separated string: "STUDENT,ADMIN"
        String rolesString = user.getRoles().stream()
                .map(role -> role.getRoleName().name()) // Assumes Role entity has getRoleName() returning an Enum
                .reduce((a, b) -> a + "," + b)
                .orElse("");
        responseData.put("roles", rolesString);


        // Audit log
        auditService.logAction(user, "LOGIN", user.getUserId().toString(), req);
        log.info("Login completed: userId={}, email={}, universityId={}, roles={}",
                user.getUserId(), user.getEmail(), user.getUniversityId(), rolesString);

        return new ApiResponse<>(true, "Login successful", responseData, System.currentTimeMillis());
    }

    @Override
    @Transactional
    public ApiResponse<Map<String, String>> verifyOtp(VerifyOtpRequest request, HttpServletRequest httpRequest,HttpServletResponse httpResponse) {
        String recipient = request.getRecipient().trim();
        String otp = request.getOtp().trim();
        TokenType type = request.getType(); // EMAIL_VERIFY or PASSWORD_RESET
        log.info("OTP verification request received: recipient={}, tokenType={}, path={}, clientIp={}",
                recipient, type, httpRequest.getRequestURI(), httpRequest.getRemoteAddr());

        User user = userRepository.findByEmail(recipient)
                .or(() -> userRepository.findByPhoneNumber(recipient.replaceAll("\\s+", "")))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        log.info("OTP verification user resolved: userId={}, email={}, phone={}, tokenType={}",
                user.getUserId(), user.getEmail(), user.getPhoneNumber(), type);

        boolean isVerified = false;

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
                log.warn("OTP verification rejected: token expired, userId={}, recipient={}, tokenType={}, expiryDate={}",
                        user.getUserId(), recipient, type, otpToken.getExpiryDate());
                verificationTokenRepository.delete(otpToken);
                throw new InvalidOtpException(
                        type == TokenType.PASSWORD_RESET
                                ? "Password reset OTP expired"
                                : "Email OTP expired"
                );
            }

            if (!passwordEncoder.matches(otp, otpToken.getToken())) {
                log.warn("OTP verification rejected: invalid email OTP, userId={}, recipient={}, tokenType={}",
                        user.getUserId(), recipient, type);
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
                log.info("Password reset OTP marked verified: userId={}, recipient={}, tokenType={}",
                        user.getUserId(), recipient, type);
            } else {
                // EMAIL_VERIFY → activate user
                user.setAccountStatus(AccountStatus.ACTIVE);
                userRepository.save(user);
                verificationTokenRepository.delete(otpToken);
                log.info("Email OTP verified and account activated: userId={}, email={}, status={}",
                        user.getUserId(), user.getEmail(), user.getAccountStatus());
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
                    log.warn("OTP verification rejected by Twilio: userId={}, recipient={}, tokenType={}, twilioStatus={}",
                            user.getUserId(), formattedPhone, type, check.getStatus());
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
                    log.info("SMS OTP verified and account activated: userId={}, phone={}, status={}",
                            user.getUserId(), user.getPhoneNumber(), user.getAccountStatus());
                } else {
                    // Twilio OTP for password reset is automatically verified, nothing else to do
                    log.info("SMS OTP verified for password reset: userId={}, phone={}",
                            user.getUserId(), user.getPhoneNumber());
                }

            } catch (Exception e) {
                log.error("OTP verification failed during Twilio check: userId={}, recipient={}, tokenType={}, error={}",
                        user.getUserId(), recipient, type, e.getMessage(), e);
                throw new InvalidOtpException("Twilio verification failed: " + e.getMessage());
            }
        }

        Map<String, String> responseMap = new HashMap<>();

        if (isVerified && type == TokenType.EMAIL_VERIFY) {
            String accessToken = jwtService.generateAccessToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);

            addRefreshTokenCookie(httpResponse, refreshToken);


            responseMap.put("accessToken", accessToken);
            // Convert ID to String to avoid Map type errors
            responseMap.put("userId", String.valueOf(user.getUserId()));
            responseMap.put("email", user.getEmail());

            return ApiResponse.success("Account activated successfully", responseMap);
        } else if (isVerified && type == TokenType.PASSWORD_RESET) {
            log.info("OTP verification completed for password reset: userId={}, recipient={}",
                    user.getUserId(), recipient);
            return ApiResponse.success("User verified for password reset", Map.of("verified", "true"));
        }

        throw new InvalidOtpException("Verification failed");
    }

    @Override
    @Transactional
    public ApiResponse<Map<String, String>> forgotPassword(ForgotPasswordRequest request, HttpServletRequest httpRequest) {
        String recipient = request.getRecipient().trim();
        log.info("Forgot password request received: recipient={}, path={}, clientIp={}",
                recipient, httpRequest.getRequestURI(), httpRequest.getRemoteAddr());
        System.out.println(recipient);

        User user = userRepository.findByEmail(recipient)
                .or(() -> userRepository.findByPhoneNumber(recipient.replaceAll("\\s+", "")))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        log.info("Forgot password user resolved: userId={}, email={}, phone={}",
                user.getUserId(), user.getEmail(), user.getPhoneNumber());

        System.out.println(user.getUsername());

        NotificationType channelType = recipient.contains("@") ? NotificationType.EMAIL : NotificationType.SMS;

        if (channelType == NotificationType.EMAIL) {
            verificationTokenRepository.deleteByUserAndType(user, TokenType.PASSWORD_RESET);
            log.info("Existing password reset email tokens cleared: userId={}, recipient={}",
                    user.getUserId(), recipient);
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
        log.info("Password reset OTP notification requested: userId={}, recipient={}, channel={}, eventType={}",
                user.getUserId(), recipient, channelType, NotificationEventType.OTP_SENT);
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
            log.info("Password reset token stored: userId={}, recipient={}, tokenType={}",
                    user.getUserId(), recipient, TokenType.PASSWORD_RESET);
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
        log.info("Reset password request received: recipient={}, path={}, clientIp={}",
                request.getRecipient(), httpRequest.getRequestURI(), httpRequest.getRemoteAddr());

        // 1️⃣ Validate input
        if(request.getRecipient() == null || request.getRecipient().isBlank() ||
                request.getNewPassword() == null || request.getNewPassword().isBlank() ||
                request.getConfirmPassword() == null || request.getConfirmPassword().isBlank()) {
            log.warn("Reset password rejected: missing required field, recipient={}, path={}, clientIp={}",
                    request.getRecipient(), httpRequest.getRequestURI(), httpRequest.getRemoteAddr());
            throw new EmptyInputBoxException("All fields are required");
        }

        if(!request.getNewPassword().equals(request.getConfirmPassword())){
            log.warn("Reset password rejected: passwords do not match, recipient={}, path={}, clientIp={}",
                    request.getRecipient(), httpRequest.getRequestURI(), httpRequest.getRemoteAddr());
            throw new WrongCredentialsException("Passwords do not match");
        }

        String recipient = request.getRecipient().trim();

        // 2️⃣ Find the user
        User user = userRepository.findByEmail(recipient)
                .or(() -> userRepository.findByPhoneNumber(recipient.replaceAll("\\s+", "")))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        log.info("Reset password user resolved: userId={}, email={}, phone={}",
                user.getUserId(), user.getEmail(), user.getPhoneNumber());

        boolean canReset = verificationTokenRepository.existsByUserAndTypeAndVerified(user, TokenType.PASSWORD_RESET, true);
        if (!canReset) {
            log.warn("Reset password rejected: password reset OTP not verified, userId={}, recipient={}",
                    user.getUserId(), recipient);
            throw new InvalidOtpException("User not verified for password reset");
        }

        // 4️⃣ Update password
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password updated: userId={}, recipient={}", user.getUserId(), recipient);

        // 5️⃣ Clean up OTP token
        verificationTokenRepository.deleteByUserAndType(user, TokenType.PASSWORD_RESET);
        log.info("Password reset token cleared: userId={}, recipient={}", user.getUserId(), recipient);

        return ApiResponse.success(
                "Password reset successfully",
                Map.of("passwordReset", "true")
        );
    }


    @Override
    // Update your AuthService interface and this implementation to accept HttpServletRequest/Response
    public ApiResponse<Map<String, String>> handleRefresh(HttpServletRequest request, HttpServletResponse response) {
        log.info("Refresh token request received: path={}, clientIp={}",
                request.getRequestURI(), request.getRemoteAddr());
        // 1. Get token from cookies
        String refreshToken = null;
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("refreshToken".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                }
            }
        }

        if (refreshToken == null || jwtService.isTokenExpired(refreshToken)) {
            log.warn("Refresh token rejected: missingOrExpired={}, path={}, clientIp={}",
                    refreshToken == null ? "missing" : "expired", request.getRequestURI(), request.getRemoteAddr());
            throw new RuntimeException("Refresh token missing or expired");
        }

        // 2. Find User and generate new pair
        String email = jwtService.extractUsername(refreshToken);
        User user = userRepository.findByEmail(email).orElseThrow();
        log.info("Refresh token user resolved: userId={}, email={}", user.getUserId(), user.getEmail());

        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);

        // 3. Update the Cookie with the new Refresh Token
        Cookie cookie = new Cookie("refreshToken", newRefreshToken);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        response.addCookie(cookie);
        log.info("Token refreshed: userId={}, email={}", user.getUserId(), user.getEmail());

        return ApiResponse.success("Token refreshed", Map.of("accessToken", newAccessToken));

    }



    @Override
    @Transactional
    public ApiResponse<Map<String, String>> resendOTP(ResendOtpRequest request) {
        String recipient = request.getRecipient().trim();
        TokenType type = request.getType(); // EMAIL_VERIFY or PASSWORD_RESET
        log.info("Resend OTP request received: recipient={}, tokenType={}", recipient, type);

        // 1. Find User
        User user = userRepository.findByEmail(recipient)
                .or(() -> userRepository.findByPhoneNumber(recipient.replaceAll("\\s+", "")))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        log.info("Resend OTP user resolved: userId={}, email={}, phone={}, tokenType={}",
                user.getUserId(), user.getEmail(), user.getPhoneNumber(), type);

        // 2. Safety Check: If account is already active and this is an EMAIL_VERIFY request
        if (type == TokenType.EMAIL_VERIFY && user.getAccountStatus() == AccountStatus.ACTIVE) {
            log.warn("Resend OTP rejected: account already active, userId={}, recipient={}, tokenType={}, status={}",
                    user.getUserId(), recipient, type, user.getAccountStatus());
            return new ApiResponse<>(false, "Account is already verified. Please login.", null, System.currentTimeMillis());
        }

        NotificationType channelType;
        String otpMessage;
        boolean isEmail = recipient.contains("@");

        if (isEmail) {
            channelType = NotificationType.EMAIL;

            // 3. EMAIL LOGIC: Clean up old tokens first
            verificationTokenRepository.deleteByUserAndType(user, type);
            log.info("Existing OTP tokens cleared before resend: userId={}, recipient={}, tokenType={}",
                    user.getUserId(), recipient, type);

            // 4. Generate New OTP
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
            log.info("New email OTP token stored: userId={}, recipient={}, tokenType={}",
                    user.getUserId(), recipient, type);
        } else {
            // 5. SMS LOGIC: Twilio handles the generation/storage internally
            channelType = NotificationType.SMS;
            otpMessage = "A new OTP has been sent via SMS";
            log.info("Resend OTP will use SMS provider storage: userId={}, recipient={}, tokenType={}",
                    user.getUserId(), recipient, type);
        }

        // 6. Send Notification (Common logic for both)
        CommonNotificationRequest notificationRequest = CommonNotificationRequest.builder()
                .userId(user.getUserId())
                .recipient(recipient)
                .title(type == TokenType.PASSWORD_RESET ? "Password Reset" : "Account Verification")
                .message(otpMessage)
                .notificationTypes(List.of(channelType))
                .eventType(NotificationEventType.OTP_SENT)
                .build();

        sendNotification.sendOTP(notificationRequest);
        log.info("Resend OTP notification requested: userId={}, recipient={}, channel={}, tokenType={}, eventType={}",
                user.getUserId(), recipient, channelType, type, NotificationEventType.OTP_SENT);

        return new ApiResponse<>(true, "Verification code resent successfully!",
                Map.of("recipient", recipient), System.currentTimeMillis());
    }

    private void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie("refreshToken", refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // Set to true in production with HTTPS
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge((int) (refreshExpiration / 1000));
        // Optional: cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }
}
