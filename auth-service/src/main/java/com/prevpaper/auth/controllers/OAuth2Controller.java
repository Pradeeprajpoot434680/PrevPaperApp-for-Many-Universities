package com.prevpaper.auth.controllers;

import com.prevpaper.auth.config.JwtService;
import com.prevpaper.auth.entities.User;
import com.prevpaper.auth.entities.Session;
import com.prevpaper.auth.repositories.SessionRepository;
import com.prevpaper.auth.repositories.UserRepository;
import com.prevpaper.auth.services.AuthService;
import com.prevpaper.comman.dto.ApiResponse;
import com.prevpaper.comman.enums.AuthProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class OAuth2Controller {
    @Autowired
    private JwtService jwtService;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @GetMapping("/oauth2/success") // Point defaultSuccessUrl here
    public void googleSuccess(OAuth2AuthenticationToken authentication, HttpServletResponse response) throws IOException {
        OAuth2User oAuth2User = authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            // ... (user creation logic) ...
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setFullName(oAuth2User.getAttribute("name"));
            newUser.setProvider(AuthProvider.GOOGLE); // Set the provider to Google
            return userRepository.save(newUser);
        });

        String accessToken = jwtService.generateAccessToken(user);

        // Pass token as a URL param so the frontend can save it
        response.sendRedirect("http://localhost:5500/index.html?token=" + accessToken);
    }

    @GetMapping("/oauth2/callback")
    public ApiResponse<Map<String,String>> googleCallback(OAuth2AuthenticationToken authentication, HttpServletResponse response){
        OAuth2User oAuth2User = authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");

        // Check if the user exists, if not create a new one
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setFullName(oAuth2User.getAttribute("name"));
            newUser.setProvider(AuthProvider.GOOGLE); // Set the provider to Google
            return userRepository.save(newUser);
        });

        // Generate JWT tokens
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        // Save the session (Refresh Token)
        Session session = Session.builder()
                .user(user)
                .refreshToken(refreshToken)
                .expiresAt(java.time.LocalDateTime.now().plusSeconds(refreshExpiration / 1000))
                .isRevoked(false)
                .build();

        // Save session to the repository
        sessionRepository.save(session);

        // Set Refresh Token in HttpOnly Cookie
        Cookie cookie = new Cookie("refreshToken", refreshToken);
        cookie.setHttpOnly(true);  // Ensures the cookie is accessible only through HTTP(S)
        cookie.setSecure(true);    // Ensure it's secure; set to false for local development (HTTP)
        cookie.setPath("/");
        cookie.setMaxAge((int) (refreshExpiration / 1000));  // Expiration time in seconds
        response.addCookie(cookie);

        // Prepare tokens for response
        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", accessToken);

        return ApiResponse.success("Login successful", tokens);
    }
}