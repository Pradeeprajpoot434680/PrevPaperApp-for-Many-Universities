package com.prevpaper.gateway.interceptor;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

@Component
@Order(-2)
@Slf4j
public class GatewayRequestLoggingFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long startTime = System.currentTimeMillis();
        String requestId = resolveRequestId(request);

        request.setAttribute("gateway.requestId", requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);
        MDC.put("requestId", requestId);

        String routeTarget = resolveRouteTarget(request.getRequestURI());
        log.info("Gateway request received: requestId={}, method={}, path={}, routeTarget={}, queryKeys={}, clientIp={}, contentType={}, contentLength={}, origin={}",
                requestId,
                request.getMethod(),
                request.getRequestURI(),
                routeTarget,
                getQueryKeys(request),
                getClientIp(request),
                request.getContentType(),
                request.getContentLengthLong(),
                request.getHeader("Origin"));

        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - startTime;
            Object userId = request.getAttribute("gateway.userId");
            Object roles = request.getAttribute("gateway.roles");
            Object universityId = request.getAttribute("gateway.universityId");
            Object scopeId = request.getAttribute("gateway.scopeId");

            log.info("Gateway request completed: requestId={}, method={}, path={}, routeTarget={}, status={}, durationMs={}, userId={}, roles={}, universityId={}, scopeId={}",
                    requestId,
                    request.getMethod(),
                    request.getRequestURI(),
                    routeTarget,
                    response.getStatus(),
                    durationMs,
                    userId,
                    roles,
                    universityId,
                    scopeId);
            MDC.remove("requestId");
        }
    }

    private String resolveRequestId(HttpServletRequest request) {
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        return requestId == null || requestId.isBlank() ? UUID.randomUUID().toString() : requestId;
    }

    private String getQueryKeys(HttpServletRequest request) {
        if (request.getParameterMap().isEmpty()) {
            return "[]";
        }
        return Collections.list(request.getParameterNames()).toString();
    }

    private String getClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String resolveRouteTarget(String path) {
        if (path.startsWith("/api/v1/auth")) {
            return "AUTH-SERVICE";
        }
        if (path.startsWith("/api/v1/get")
                || path.startsWith("/api/v1/global-admin")
                || path.startsWith("/api/v1/university-rep")
                || path.startsWith("/api/v1/department-rep")
                || path.startsWith("/api/v1/program-rep")
                || path.startsWith("/api/v1/session-rep")) {
            return "UNIVERSITY-SERVICE";
        }
        if (path.startsWith("/api/v1/notifications")) {
            return "NOTIFICATION-SERVICE";
        }
        if (path.startsWith("/api/v1/users")) {
            return "USER-SERVICE";
        }
        if (path.startsWith("/api/v1/content")) {
            return "CONTENT-SERVICE";
        }
        return "UNKNOWN";
    }
}
