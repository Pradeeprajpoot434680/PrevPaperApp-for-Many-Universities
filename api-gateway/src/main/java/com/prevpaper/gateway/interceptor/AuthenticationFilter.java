package com.prevpaper.gateway.interceptor;

import com.prevpaper.comman.dto.AuthResponse;
import com.prevpaper.comman.service.RedisService;
import com.prevpaper.gateway.client.AuthClient;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;

@Component
@Order(-1)
@Slf4j
public class AuthenticationFilter extends OncePerRequestFilter {

    private final AuthClient authClient;
    private final RedisService redisService;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    // 🟢 Preserves insertion order: Specific paths MUST be evaluated before broad fallback paths
    private final Map<String, String> roleRequirements = new LinkedHashMap<>();

    private final List<String> openEndpoints = List.of(
            "/api/v1/auth/**",
            "/api/v1/universities/exists",
            "/api/v1/get/universities",
            "/api/v1/get/departments/**",
            "/api/v1/auth/refresh",
            "/api/v1/get/**",
            "/actuator/**"
    );

    private final List<String> UPLOADER_ROLES = List.of(
            "STUDENT", "SESSION_REP", "PROGRAM_REP", "DEPT_REP", "UNIVERSITY_ADMIN", "GLOBAL_ADMIN"
    );

    public AuthenticationFilter(AuthClient authClient, RedisService redisService) {
        this.authClient = authClient;
        this.redisService = redisService;

        // Populate role requirements in strict order of specificity
        roleRequirements.put("/api/v1/global-admin", "GLOBAL_ADMIN");
        roleRequirements.put("/api/v1/university-rep", "UNIVERSITY_ADMIN");
        roleRequirements.put("/api/v1/department-rep", "DEPT_REP");
        roleRequirements.put("/api/v1/program-rep", "PROGRAM_REP");
        roleRequirements.put("/api/v1/session-rep", "SESSION_REP");
        roleRequirements.put("/api/v1/users/internal/store", "STUDENT");
        roleRequirements.put("/api/v1/users/me/profile", "STUDENT");
        roleRequirements.put("/api/v1/content", "STUDENT");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 1. Handle CORS Preflight Requests
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        Object requestId = getRequestId(request);

        // 2. Pass open/public endpoints directly
        boolean isOpen = openEndpoints.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
        if (isOpen) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Extract & Validate Bearer Token Header
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            handleError(response, "Missing Authorization Header", 401);
            return;
        }

        String token = authHeader.substring(7);
        AuthResponse authInfo = null;

        try {
            String redisTokenKey = "token:val:" + token;
            authInfo = redisService.get(redisTokenKey, AuthResponse.class);

            if (authInfo != null) {
                log.debug("Redis Cache HIT for token. RequestId={}", requestId);
            } else {
                log.info("Redis Cache MISS for token. Calling AUTH-SERVICE. RequestId={}", requestId);
                authInfo = authClient.validateToken(token);

                if (authInfo != null && authInfo.isValid()) {
                    redisService.set(redisTokenKey, authInfo, 300L);
                }
            }

            if (authInfo == null || !authInfo.isValid()) {
                handleError(response, "Token invalid or expired", 401);
                return;
            }

            // 🟢 Handle users with multiple roles correctly
            List<String> userRoles = authInfo.roles() != null ? authInfo.roles() : Collections.emptyList();
            boolean pathMatched = false;

            for (Map.Entry<String, String> entry : roleRequirements.entrySet()) {
                if (path.startsWith(entry.getKey())) {
                    pathMatched = true;
                    String requiredRole = entry.getValue();
                    boolean isAuthorized = false;

                    if (path.startsWith("/api/v1/content")) {
                        isAuthorized = userRoles.stream().anyMatch(UPLOADER_ROLES::contains);
                    } else {
                        // 🟢 Authorized if user possesses the required role OR possesses GLOBAL_ADMIN
                        isAuthorized = userRoles.contains(requiredRole) || userRoles.contains("GLOBAL_ADMIN");
                    }

                    if (!isAuthorized) {
                        log.warn("Authorization failure: Path={}, RequiredRole={}, UserRoles={}", path, requiredRole, userRoles);
                        handleError(response, "Access Denied: Missing role permissions", 403);
                        return;
                    }

                    // 4. Multi-Tenant Scope Validation (Handles multi-role accounts safely)
                    if (!userRoles.contains("GLOBAL_ADMIN")) {
                        String targetIdInUrl = extractUuidFromPath(path);

                        if (targetIdInUrl != null) {
                            // 🟢 University-scoped routes (e.g., /university-rep/{uniId}, /content/internal/stats/university/{uniId})
                            if (path.contains("/university") || path.contains("/stats/university/")) {
                                if (!targetIdInUrl.equalsIgnoreCase(authInfo.universityId())) {
                                    log.warn("University Scope Mismatch! targetInUrl={}, userUniId={}", targetIdInUrl, authInfo.universityId());
                                    handleError(response, "Access Denied: University scope mismatch", 403);
                                    return;
                                }
                            }
                            // 🟢 Management-scoped routes (e.g., /program-rep/{scopeId}, /department-rep/{scopeId})
                            else if (path.contains("-rep/")) {
                                boolean isUniAdminAuthorized = userRoles.contains("UNIVERSITY_ADMIN") && targetIdInUrl.equalsIgnoreCase(authInfo.universityId());
                                boolean isScopeRepAuthorized = targetIdInUrl.equalsIgnoreCase(authInfo.scopeId());

                                if (!isUniAdminAuthorized && !isScopeRepAuthorized) {
                                    log.warn("Management Scope Mismatch! targetInUrl={}, userScopeId={}, userUniId={}", targetIdInUrl, authInfo.scopeId(), authInfo.universityId());
                                    handleError(response, "Access Denied: Management scope mismatch", 403);
                                    return;
                                }
                            }
                        }
                    }
                    break;
                }
            }

            if (!pathMatched && !userRoles.contains("GLOBAL_ADMIN")) {
                handleError(response, "Access Denied: Secure fallback path blocking", 403);
                return;
            }

            // 5. Inject Security Context Headers for Downstream Microservices
            Map<String, String> customHeaders = new HashMap<>();
            customHeaders.put("X-User-Id", String.valueOf(authInfo.userId()));
            customHeaders.put("X-User-Roles", String.join(",", userRoles));
            customHeaders.put("X-User-Email", authInfo.email() != null ? authInfo.email() : "");
            customHeaders.put("X-University-Id", authInfo.universityId() != null ? authInfo.universityId() : "");
            customHeaders.put("X-Scope-Id", authInfo.scopeId() != null ? authInfo.scopeId() : "");

            request.setAttribute("gateway.userId", authInfo.userId());
            request.setAttribute("gateway.roles", userRoles);
            request.setAttribute("gateway.universityId", authInfo.universityId());
            request.setAttribute("gateway.scopeId", authInfo.scopeId());

            HttpServletRequest wrappedRequest = new HttpServletRequestWrapper(request) {
                @Override public String getHeader(String name) {
                    return customHeaders.getOrDefault(name, super.getHeader(name));
                }
                @Override public Enumeration<String> getHeaders(String name) {
                    if (customHeaders.containsKey(name)) {
                        return Collections.enumeration(List.of(customHeaders.get(name)));
                    }
                    return super.getHeaders(name);
                }
                @Override public Enumeration<String> getHeaderNames() {
                    Set<String> headerNames = new HashSet<>(customHeaders.keySet());
                    Enumeration<String> original = super.getHeaderNames();
                    while (original != null && original.hasMoreElements()) {
                        headerNames.add(original.nextElement());
                    }
                    return Collections.enumeration(headerNames);
                }
            };

            filterChain.doFilter(wrappedRequest, response);

        } catch (Exception e) {
            log.error("Gateway auth interceptor error: requestId={}, error={}", requestId, e.getMessage(), e);
            handleError(response, "Security Infrastructure Failure", 503);
        }
    }

    /**
     * 🟢 Reliably extracts UUIDs from anywhere in the URL path segment array
     */
    private String extractUuidFromPath(String path) {
        String[] segments = path.split("/");
        for (String segment : segments) {
            if (segment.length() == 36 && segment.contains("-")) {
                return segment;
            }
        }
        return null;
    }

    private void handleError(HttpServletResponse response, String message, int status) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setHeader("Access-Control-Allow-Origin", "http://localhost:5173");
        response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, PATCH");
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
        response.getWriter().flush();
    }

    private Object getRequestId(HttpServletRequest request) {
        Object requestId = request.getAttribute("gateway.requestId");
        return requestId == null ? "not-set" : requestId;
    }
}