


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
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;

@Component
@Order(-1)
@Slf4j
public class AuthenticationFilter extends OncePerRequestFilter {

    private final AuthClient authClient;

    private final Map<String, String> roleRequirements = Map.of(
            "/api/v1/global-admin", "GLOBAL_ADMIN",
            "/api/v1/university-rep", "UNIVERSITY_ADMIN",
            "/api/v1/department-rep", "DEPT_REP",
            "/api/v1/program-rep", "PROGRAM_REP",
            "/api/v1/session-rep", "SESSION_REP"
    );

    private final List<String> openEndpoints = List.of(
            "/api/v1/auth/signup",
            "/api/v1/auth/login",
            "/api/v1/auth/hello",
            "/api/v1/universities/exists"
    );

    public AuthenticationFilter(AuthClient authClient) {
        this.authClient = authClient;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        if (openEndpoints.stream().anyMatch(path::startsWith)) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            handleError(response, "Missing Authorization Header", 401);
            return;
        }

        String token = authHeader.substring(7);

        try {

            AuthResponse authInfo = authClient.validateToken(token);

            if (!authInfo.isValid()) {
                handleError(response, "Token invalid or expired", 401);
                return;
            }

            if (!authInfo.isVerified()) {
                handleError(response, "Account not verified", 403);
                return;
            }

            // ---- ROLE + SCOPE VALIDATION ----

            for (Map.Entry<String, String> entry : roleRequirements.entrySet()) {

                if (path.startsWith(entry.getKey())) {

                    String requiredRole = entry.getValue();

                    // Role Check
                    if (!authInfo.roles().contains(requiredRole)) {
                        handleError(response, "Access Denied: Required role " + requiredRole, 403);
                        return;
                    }

                    // Skip scope validation for GLOBAL_ADMIN
                    if (!authInfo.roles().contains("GLOBAL_ADMIN")) {

                        String targetIdInUrl = extractIdFromPath(path, 4);

                        if ("UNIVERSITY_ADMIN".equals(requiredRole)) {

                            if (targetIdInUrl != null &&
                                    !targetIdInUrl.equals(authInfo.universityId())) {

                                handleError(response, "University mismatch", 403);
                                return;
                            }
                        }

                        else if ("DEPT_REP".equals(requiredRole)
                                || "PROGRAM_REP".equals(requiredRole)
                                || "SESSION_REP".equals(requiredRole)) {

                            if (targetIdInUrl != null &&
                                    !targetIdInUrl.equals(authInfo.scopeId())) {

                                handleError(response,
                                        "Scope mismatch. Resource not allowed.",
                                        403);
                                return;
                            }
                        }
                    }
                }
            }

            // ---- ADD HEADERS TO REQUEST ----

            Map<String, String> customHeaders = new HashMap<>();
            customHeaders.put("X-User-Id", authInfo.userId());
            customHeaders.put("X-User-Roles", String.join(",", authInfo.roles()));
            customHeaders.put("X-User-Email", authInfo.email());
            customHeaders.put("X-University-Id", authInfo.universityId());
            customHeaders.put("X-Scope-Id", authInfo.scopeId());

            HttpServletRequest wrappedRequest = new HttpServletRequestWrapper(request) {

                @Override
                public String getHeader(String name) {

                    if (customHeaders.containsKey(name)) {
                        return customHeaders.get(name);
                    }

                    return super.getHeader(name);
                }

                @Override
                public Enumeration<String> getHeaders(String name) {

                    if (customHeaders.containsKey(name)) {
                        return Collections.enumeration(
                                List.of(customHeaders.get(name))
                        );
                    }

                    return super.getHeaders(name);
                }

                @Override
                public Enumeration<String> getHeaderNames() {

                    Set<String> headerNames = new HashSet<>(customHeaders.keySet());

                    Enumeration<String> original = super.getHeaderNames();

                    while (original.hasMoreElements()) {
                        headerNames.add(original.nextElement());
                    }

                    return Collections.enumeration(headerNames);
                }
            };

//            log.info("Gateway Auth Success User={} Roles={} Scope={}",
//                    authInfo.userId(),
//                    authInfo.roles(),
//                    authInfo.scopeId());

            filterChain.doFilter(wrappedRequest, response);

        }

        catch (Exception e) {

//            log.error("Security Gateway Error: {}", e.getMessage());

            handleError(response,
                    "Security Service Unavailable",
                    503);
        }
    }

    private void handleError(HttpServletResponse response,
                             String message,
                             int status) throws IOException {

        response.setStatus(status);
        response.setContentType("application/json");

        response.getWriter().write("{\"error\":\"" + message + "\"}");
        response.getWriter().flush();
    }

    private String extractIdFromPath(String path, int index) {

        String[] segments = path.split("/");

        return segments.length > index ? segments[index] : null;
    }
}