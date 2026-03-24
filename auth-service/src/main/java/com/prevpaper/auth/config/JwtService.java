package com.prevpaper.auth.config;

import com.prevpaper.auth.entities.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-expiration}")
    private long accessExpiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    private SecretKey signingKey; // Changed from Key to SecretKey

    @PostConstruct
    public void init() {
        // JJWT 0.12.x requires a SecretKey for HMAC algorithms
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getUserId());
        claims.put("roles", user.getRoles()
                .stream()
                .map(role -> role.getRoleName().name())
                .collect(Collectors.toList()));
        claims.put("universityId", user.getUniversityId());
        claims.put("scopeId", user.getAssignedScopeId());
        return buildToken(claims, user.getEmail(), accessExpiration);
    }

    public String generateRefreshToken(User user) {
        return buildToken(new HashMap<>(), user.getEmail(), refreshExpiration);
    }

    private String buildToken(Map<String, Object> claims, String subject, long expirationMillis) {
        return Jwts.builder()
                .claims(claims)           // 0.12.x uses .claims() instead of .setClaims()
                .subject(subject)         // 0.12.x uses .subject() instead of .setSubject()
                .issuedAt(new Date())     // 0.12.x uses .issuedAt() instead of .setIssuedAt()
                .expiration(new Date(System.currentTimeMillis() + expirationMillis))
                .signWith(signingKey)     // Algorithm is automatically inferred from the key
                .compact();
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)   // No casting needed if signingKey is SecretKey
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateTokenOnly(String token) {
        try {
            // This will throw an exception if the signature is invalid or token is expired
            extractAllClaims(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            // Log error if needed: "Token validation failed: " + e.getMessage()
            return false;
        }
    }


    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    public boolean isTokenValid(String token, String username) {
        final String extractedUsername = extractUsername(token);
        return extractedUsername.equals(username) && !isTokenExpired(token);
    }
}
