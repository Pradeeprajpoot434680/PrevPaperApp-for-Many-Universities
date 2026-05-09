


package com.prevpaper.gateway.interceptor;

import com.prevpaper.comman.dto.AuthResponse;
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
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private final Map<String, String> roleRequirements = Map.of(
            "/api/v1/global-admin", "GLOBAL_ADMIN",
            "/api/v1/university-rep", "UNIVERSITY_ADMIN",
            "/api/v1/department-rep", "DEPT_REP",
            "/api/v1/program-rep", "PROGRAM_REP",
            "/api/v1/session-rep", "SESSION_REP",
            "/api/v1/users/internal/store", "STUDENT",
            "/api/v1/user/me/profile","STUDENT"
    );

    private final List<String> openEndpoints = List.of(
            "/api/v1/auth/**",
            "/api/v1/universities/exists",
            "/api/v1/get/universities",
            "/api/v1/get/departments/**",
            "/api/v1/auth/refresh",
            "/api/v1/get/**"
    );

    private final List<String> UPLOADER_ROLES = List.of(
            "STUDENT", "SESSION_REP", "PROGRAM_REP", "DEPT_REP", "UNIVERSITY_ADMIN", "GLOBAL_ADMIN"
    );

    public AuthenticationFilter(AuthClient authClient) {
        this.authClient = authClient;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 1. Handle Preflight
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            log.info("Gateway auth skipped for preflight request: requestId={}, method={}, path={}",
                    getRequestId(request), request.getMethod(), request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        log.info("Gateway auth check started: requestId={}, method={}, path={}",
                getRequestId(request), request.getMethod(), path);

        // 2. Handle Open Endpoints
        boolean isOpen = openEndpoints.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
        if (isOpen) {
            log.info("Gateway auth skipped for open endpoint: requestId={}, method={}, path={}",
                    getRequestId(request), request.getMethod(), path);
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Extract Token
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Gateway auth rejected: requestId={}, method={}, path={}, reason=missing_authorization_header",
                    getRequestId(request), request.getMethod(), path);
            handleError(response, "Missing Authorization Header", 401);
            return;
        }

        String token = authHeader.substring(7);

        try {
            // 4. Validate Token (SAFE CHECK)
            log.info("Gateway token validation requested: requestId={}, method={}, path={}",
                    getRequestId(request), request.getMethod(), path);
            AuthResponse authInfo = authClient.validateToken(token);

            if (authInfo == null) {
                throw new RuntimeException("Auth Service returned empty response");
            }

            if (!authInfo.isValid()) {
                log.warn("Gateway auth rejected: requestId={}, method={}, path={}, reason=invalid_or_expired_token",
                        getRequestId(request), request.getMethod(), path);
                handleError(response, "Token invalid or expired", 401);
                return;
            }

            // 5. Role & Scope Validation
            List<String> userRoles = authInfo.roles() != null ? authInfo.roles() : Collections.emptyList();
            log.info("Gateway token validated: requestId={}, userId={}, roles={}, universityId={}, scopeId={}",
                    getRequestId(request), authInfo.userId(), userRoles, authInfo.universityId(), authInfo.scopeId());

            for (Map.Entry<String, String> entry : roleRequirements.entrySet()) {

                if (path.startsWith(entry.getKey())) {
                    String requiredRole = entry.getValue();
                    boolean isAuthorized = false;
                    log.info("Gateway role check started: requestId={}, userId={}, path={}, requiredRole={}, userRoles={}",
                            getRequestId(request), authInfo.userId(), path, requiredRole, userRoles);

                    if (!userRoles.contains(requiredRole)) {
                        log.warn("Gateway auth rejected: requestId={}, userId={}, path={}, reason=missing_role, requiredRole={}, userRoles={}",
                                getRequestId(request), authInfo.userId(), path, requiredRole, userRoles);
                        handleError(response, "Access Denied: Missing role " + requiredRole, 403);
                        return;
                    }

                    if (path.startsWith("/api/v1/content")) {
                        isAuthorized = userRoles.stream().anyMatch(UPLOADER_ROLES::contains);
                    } else {
                        // Standard check for other specific admin routes
                        isAuthorized = userRoles.contains(requiredRole);
                    }

                    if (!isAuthorized) {
                        log.warn("Gateway auth rejected: requestId={}, userId={}, path={}, reason=not_authorized_for_route, requiredRole={}, userRoles={}",
                                getRequestId(request), authInfo.userId(), path, requiredRole, userRoles);
                        handleError(response, "Access Denied: Missing required role for " + path, 403);
                        return;
                    }


                    // Scope checks for non-Global Admins
                    if (!userRoles.contains("GLOBAL_ADMIN")) {
                        String targetIdInUrl = extractIdFromPath(path, 4);

                        if ("UNIVERSITY_ADMIN".equals(requiredRole)) {
                            if (targetIdInUrl != null && !targetIdInUrl.equals(authInfo.universityId())) {
                                log.warn("Gateway auth rejected: requestId={}, userId={}, path={}, reason=university_scope_mismatch, targetId={}, userUniversityId={}",
                                        getRequestId(request), authInfo.userId(), path, targetIdInUrl, authInfo.universityId());
                                handleError(response, "University mismatch", 403);
                                return;
                            }
                        } else if (List.of("DEPT_REP", "PROGRAM_REP", "SESSION_REP").contains(requiredRole)) {
                            if (targetIdInUrl != null && !targetIdInUrl.equals(authInfo.scopeId())) {
                                log.warn("Gateway auth rejected: requestId={}, userId={}, path={}, reason=scope_mismatch, targetId={}, userScopeId={}",
                                        getRequestId(request), authInfo.userId(), path, targetIdInUrl, authInfo.scopeId());
                                handleError(response, "Scope mismatch", 403);
                                return;
                            }
                        }
                    }
                    log.info("Gateway authorization passed: requestId={}, userId={}, path={}, requiredRole={}, universityId={}, scopeId={}",
                            getRequestId(request), authInfo.userId(), path, requiredRole, authInfo.universityId(), authInfo.scopeId());
                    break; // Exit loop once path is matched and validated
                }
            }

            // 6. Header Injection (NULL-SAFE)
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
            log.info("Gateway user context injected: requestId={}, userId={}, roles={}, universityId={}, scopeId={}",
                    getRequestId(request), authInfo.userId(), userRoles, authInfo.universityId(), authInfo.scopeId());

            HttpServletRequest wrappedRequest = new HttpServletRequestWrapper(request) {
                @Override
                public String getHeader(String name) {
                    return customHeaders.getOrDefault(name, super.getHeader(name));
                }

                @Override
                public Enumeration<String> getHeaders(String name) {
                    if (customHeaders.containsKey(name)) {
                        return Collections.enumeration(List.of(customHeaders.get(name)));
                    }
                    return super.getHeaders(name);
                }

                @Override
                public Enumeration<String> getHeaderNames() {
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
            log.error("Gateway auth service error: requestId={}, method={}, path={}, error={}",
                    getRequestId(request), request.getMethod(), path, e.getMessage(), e);
            handleError(response, "Security Service Unavailable: " + e.getMessage(), 503);
        }
    }

    private void handleError(HttpServletResponse response,
                             String message,
                             int status) throws IOException {

        response.setStatus(status);
        response.setContentType("application/json");

        // Add CORS headers
        response.setHeader("Access-Control-Allow-Origin", "http://localhost:5173");
        response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS,PATCH");
        response.setHeader("Access-Control-Allow-Credentials", "true");
        // Inside AuthenticationFilter.java -> handleError method
        response.getWriter().write("{\"error\":\"" + message + "\"}");
        response.getWriter().flush();
    }

    private Object getRequestId(HttpServletRequest request) {
        Object requestId = request.getAttribute("gateway.requestId");
        return requestId == null ? "not-set" : requestId;
    }

    private String extractIdFromPath(String path, int index) {

        String[] segments = path.split("/");

        return segments.length > index ? segments[index] : null;
    }
}
