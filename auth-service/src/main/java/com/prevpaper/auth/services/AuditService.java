package com.prevpaper.auth.services;


import com.prevpaper.auth.entities.AuditLog;
import com.prevpaper.auth.entities.User;
import com.prevpaper.auth.repositories.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public void logAction(User actor, String action, String targetId, HttpServletRequest request) {
        AuditLog log = AuditLog.builder()
                .actor(actor)
                .action(action)
                .targetId(targetId)
                .ipAddress(getClientIP(request))
                .httpMethod(request.getMethod())
                .url(request.getRequestURI())
                .userAgent(request.getHeader("User-Agent"))
                .build();

        auditLogRepository.save(log);
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0]; // first IP if multiple
    }
}