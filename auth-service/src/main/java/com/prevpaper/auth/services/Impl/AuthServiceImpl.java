package com.prevpaper.auth.services.Impl;

import com.prevpaper.auth.config.JwtService;
import com.prevpaper.auth.dto.LoginRequest;
import com.prevpaper.auth.dto.SignupRequest;
import com.prevpaper.auth.dto.VerifyOtpRequest;
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
import com.prevpaper.comman.dto.ApiResponse;
import com.prevpaper.comman.enums.AccountStatus;
import com.prevpaper.comman.enums.AuthProvider;
import com.prevpaper.comman.enums.TokenType;
import com.prevpaper.comman.enums.UserRole;

// CORRECTED IMPORTS: Use jakarta instead of javax
import com.prevpaper.comman.exception.InvalidOtpException;
import com.prevpaper.comman.exception.ResourceNotFoundException;
import com.prevpaper.comman.exception.RoleExceptionHandler;
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

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtService jwtService;
    private final SessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private  final VerificationTokenRepository verificationTokenRepository;
    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    // Signup Method
    @Override
    @Transactional
    public ApiResponse<Map<String, String>> registerUser(SignupRequest request, HttpServletRequest req) {
        // 1. Validation
        if ((request.getEmail() == null || request.getEmail().isBlank()) &&
                (request.getPhoneNumber() == null || request.getPhoneNumber().isBlank())) {
            return new ApiResponse<>(false, "Email or Phone Number is required", null, System.currentTimeMillis());
        }

        // 2. Duplicate Check
        if (request.getEmail() != null && userRepository.existsByEmail(request.getEmail())) {
            return new ApiResponse<>(false, "Email already exists", null, System.currentTimeMillis());
        }
        if (request.getPhoneNumber() != null && userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            return new ApiResponse<>(false, "Phone number already exists", null, System.currentTimeMillis());
        }

        // 3. Fetch default role
        Role studentRole = roleRepository.findByRoleName(UserRole.STUDENT)
                .orElseThrow(() -> new RoleExceptionHandler("Default Role not found"));

        // 4. Create User
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



        // 5. Generate Tokens
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        // NEW: Generate 6-digit OTP
        String otp = String.valueOf((int) (Math.random() * 900000) + 100000);

        // send otp

        verificationTokenRepository.save(
                VerificationToken.builder()
                        .token(otp)
                        .user(user)
                        .type(TokenType.EMAIL_VERIFY)
                        .expiryDate(LocalDateTime.now().plusMinutes(10)) // 10 min expiry
                        .build()
        );

        // todo for send the otp

        // if otp is verify successfully we will generate token and send to frontend otherwise not

        // 6. Store Session

        sessionRepository.save(
                Session.builder()
                        .user(user)
                        .refreshToken(refreshToken)
                        .expiresAt(LocalDateTime.now().plusSeconds(refreshExpiration / 1000))
                        .isRevoked(false)
                        .build()
        );

        // 7. Prepare tokens
        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", accessToken);
        tokens.put("refreshToken", refreshToken);

        // 8. Audit log
        auditService.logAction(user, "SIGNUP", user.getUserId().toString(), req);

        // 9. Return response
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

        VerificationToken otpToken = verificationTokenRepository
                .findByUser(user)
                .orElseThrow(() -> new InvalidOtpException("OTP not found"));

        System.out.println("User found"+ user);
        System.out.println("Verification TOkan :" + otpToken);


        // Check OTP match
        if (!otpToken.getToken().equals(request.getOtp())) {
            throw new InvalidOtpException("Invalid OTP");
        }

        // Check expiry
        if (otpToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new InvalidOtpException("OTP expired");
        }

        // Activate account
        user.setAccountStatus(AccountStatus.ACTIVE);
        userRepository.save(user);

        // Delete OTP after successful verification
        verificationTokenRepository.delete(otpToken);

        return ApiResponse.success(
                "OTP verified successfully",
                Map.of("verified", "true")
        );
    }
}