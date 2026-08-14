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
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@Order(-3)
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RedisService redisService;
    private final RateLimitingProperties properties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * Open endpoints that bypass rate limiting entirely.
     * These are health checks and public info endpoints that should never be throttled.
     */
    private static final String[] OPEN_ENDPOINTS = {
            "/actuator/health",
            "/actuator/info"
    };

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

        // 4. Find matching route configuration (first match wins, order matters)
        RateLimitingProperties.RouteLimitConfig routeConfig = findRouteConfig(path);
        if (routeConfig == null) {
            routeConfig = properties.getDefaultConfig();
        }

        // 5. Extract client IP (respecting X-Forwarded-For from nginx proxy)
        String clientIp = extractClientIp(request);

        // 6. Apply IP-based rate limiting (atomic Redis INCR)
        String ipKey = buildKey("ip", path, clientIp, routeConfig.getIpWindowSeconds());
        if (!checkRateLimit(ipKey, routeConfig.getIpLimit(), routeConfig.getIpWindowSeconds())) {
            log.warn("Rate limit exceeded for IP={}, path={}, limit={}/{}s",
                    clientIp, path, routeConfig.getIpLimit(), routeConfig.getIpWindowSeconds());
            handleRateLimitExceeded(response, routeConfig.getIpLimit(), routeConfig.getIpWindowSeconds());
            return;
        }

        // 7. Apply user-based rate limiting (if authenticated)
        String userId = (String) request.getAttribute("gateway.userId");
        if (userId != null) {
            String userKey = buildKey("user", path, userId, routeConfig.getUserWindowSeconds());
            if (!checkRateLimit(userKey, routeConfig.getUserLimit(), routeConfig.getUserWindowSeconds())) {
                log.warn("Rate limit exceeded for userId={}, path={}, limit={}/{}s",
                        userId, path, routeConfig.getUserLimit(), routeConfig.getUserWindowSeconds());
                handleRateLimitExceeded(response, routeConfig.getUserLimit(), routeConfig.getUserWindowSeconds());
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Atomic fixed-window rate limit check using Redis INCR.
     * Uses INCR + EXPIRE pattern for thread-safe and distributed-safe counting.
     */
    private boolean checkRateLimit(String key, int limit, int windowSeconds) {
        try {
            Long currentCount = redisService.increment(key);

            if (currentCount == null) {
                // Redis unavailable: fail open to avoid blocking legitimate traffic
                log.error("Rate limit Redis increment failed for key={}, failing open", key);
                return true;
            }

            // First request in window: set TTL so the counter auto-expires
            if (currentCount == 1) {
                boolean ttlSet = redisService.expire(key, windowSeconds, TimeUnit.SECONDS);
                if (!ttlSet) {
                    log.warn("Failed to set TTL on rate limit key={}", key);
                }
            }

            if (currentCount > limit) {
                return false; // Limit exceeded
            }

            return true;

        } catch (Exception e) {
            log.error("Rate limit check failed for key={}: {}", key, e.getMessage());
            // Fail open: allow request if Redis is unavailable
            return true;
        }
    }

    private boolean isOpenEndpoint(String path) {
        for (String openEndpoint : OPEN_ENDPOINTS) {
            if (pathMatcher.match(openEndpoint, path)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Find the first matching route configuration.
     * Routes are evaluated in insertion order (LinkedHashMap).
     */
    private RateLimitingProperties.RouteLimitConfig findRouteConfig(String path) {
        for (Map.Entry<String, RateLimitingProperties.RouteLimitConfig> entry : properties.getRoutes().entrySet()) {
            if (pathMatcher.match(entry.getKey(), path)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // X-Forwarded-For can contain multiple IPs: client, proxy1, proxy2
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String buildKey(String type, String path, String identifier, int windowSeconds) {
        // Key format: rate_limit:{type}:{path_hash}:{identifier}:{window_timestamp}
        // Using path hash to keep keys reasonably sized
        long windowTimestamp = System.currentTimeMillis() / (windowSeconds * 1000L);
        int pathHash = path.replace("/", "_").hashCode();
        return String.format("rate_limit:%s:%d:%s:%d", type, pathHash, identifier, windowTimestamp);
    }

    private void handleRateLimitExceeded(HttpServletResponse response, int limit, int windowSeconds) throws IOException {
        response.setStatus(429); // HTTP 429 Too Many Requests
        response.setContentType("application/json");
        response.setHeader("Retry-After", String.valueOf(windowSeconds));
        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Window", String.valueOf(windowSeconds));
        response.getWriter().write("{\"error\":\"Rate limit exceeded. Please try again later.\"}");
        response.getWriter().flush();
    }
}
