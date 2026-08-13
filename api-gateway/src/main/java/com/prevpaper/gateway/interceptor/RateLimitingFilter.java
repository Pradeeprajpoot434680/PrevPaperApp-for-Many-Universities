package com.prevpaper.gateway.interceptor;

import com.prevpaper.comman.service.RedisService;
import com.prevpaper.gateway.config.RateLimitingProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;

@Component
@Order(-3)
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RedisService redisService;
    private final RateLimitingProperties properties;

    // 🟢 Open endpoints that bypass rate limiting (health checks, public info)
    private static final Set<String> OPEN_ENDPOINTS = Set.of(
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/verify-otp",
            "/api/v1/auth/resend-otp",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/api/v1/universities/exists",
            "/api/v1/get/universities",
            "/api/v1/get/departments",
            "/actuator/health",
            "/actuator/info"
    );

    // 🟢 Paths that map to rate limit categories
    private static final Map<String, String> PATH_CATEGORIES = Map.of(
            "/api/v1/auth", "auth",
            "/api/v1/upload", "upload"
    );

    public RateLimitingFilter(RedisService redisService, RateLimitingProperties properties) {
        this.redisService = redisService;
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 1. Skip if rate limiting is disabled
        if (!properties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        String method = request.getMethod();

        // 2. Skip OPTIONS preflight requests
        if ("OPTIONS".equalsIgnoreCase(method)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Skip explicitly open endpoints
        if (isOpenEndpoint(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 4. Determine rate limit category
        String category = getCategory(path);
        RateLimitingProperties.LimitConfig ipConfig = properties.getIp().getOrDefault(
                category, properties.getIp().get("default"));
        RateLimitingProperties.LimitConfig userConfig = properties.getUser().getOrDefault(
                category, properties.getUser().get("default"));

        // 5. Extract client IP (respecting X-Forwarded-For from nginx proxy)
        String clientIp = extractClientIp(request);

        // 6. Apply IP-based rate limiting
        String ipKey = buildKey("ip", category, clientIp, ipConfig.getWindowSeconds());
        if (!checkRateLimit(ipKey, ipConfig)) {
            log.warn("Rate limit exceeded for IP={}, path={}, category={}, limit={}/{}s",
                    clientIp, path, category, ipConfig.getLimit(), ipConfig.getWindowSeconds());
            handleRateLimitExceeded(response, ipConfig);
            return;
        }

        // 7. Apply user-based rate limiting (if authenticated)
        String userId = (String) request.getAttribute("gateway.userId");
        if (userId != null) {
            String userKey = buildKey("user", category, userId, userConfig.getWindowSeconds());
            if (!checkRateLimit(userKey, userConfig)) {
                log.warn("Rate limit exceeded for userId={}, path={}, category={}, limit={}/{}s",
                        userId, path, category, userConfig.getLimit(), userConfig.getWindowSeconds());
                handleRateLimitExceeded(response, userConfig);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 🟢 Fixed-window rate limit check using Redis
     * Uses get + set pattern (consider adding atomic INCR to RedisService for production)
     */
    private boolean checkRateLimit(String key, RateLimitingProperties.LimitConfig config) {
        try {
            Long currentCount = redisService.get(key, Long.class);

            if (currentCount == null) {
                // First request in this window: initialize counter with TTL
                redisService.set(key, 1L, (long) config.getWindowSeconds());
                return true;
            }

            if (currentCount >= config.getLimit()) {
                return false; // Limit exceeded
            }

            // Increment counter
            redisService.set(key, currentCount + 1, (long) config.getWindowSeconds());
            return true;

        } catch (Exception e) {
            log.error("Rate limit check failed for key={}: {}", key, e.getMessage());
            // Fail open: allow request if Redis is unavailable
            return true;
        }
    }

    private boolean isOpenEndpoint(String path) {
        return OPEN_ENDPOINTS.stream().anyMatch(path::equals);
    }

    private String getCategory(String path) {
        for (Map.Entry<String, String> entry : PATH_CATEGORIES.entrySet()) {
            if (path.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return "default";
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // X-Forwarded-For can contain multiple IPs: client, proxy1, proxy2
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String buildKey(String type, String category, String identifier, int windowSeconds) {
        // Key format: rate_limit:{type}:{category}:{identifier}:{window_timestamp}
        long windowTimestamp = System.currentTimeMillis() / (windowSeconds * 1000L);
        return String.format("rate_limit:%s:%s:%s:%d", type, category, identifier, windowTimestamp);
    }

    private void handleRateLimitExceeded(HttpServletResponse response, RateLimitingProperties.LimitConfig config) throws IOException {
        response.setStatus(429); // HTTP 429 Too Many Requests
        response.setContentType("application/json");
        response.setHeader("Retry-After", String.valueOf(config.getWindowSeconds()));
        response.setHeader("X-RateLimit-Limit", String.valueOf(config.getLimit()));
        response.setHeader("X-RateLimit-Window", String.valueOf(config.getWindowSeconds()));
        response.getWriter().write("{\"error\":\"Rate limit exceeded. Please try again later.\"}");
        response.getWriter().flush();
    }
}
