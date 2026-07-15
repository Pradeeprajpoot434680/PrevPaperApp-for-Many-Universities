package com.prevpaper.gateway.client;

import com.prevpaper.comman.dto.AuthResponse; // Ensure this import is present
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "AUTH-SERVICE",
        url = "${auth.service.url:http://auth-service:8081}"
)
public interface AuthClient {

    // 🟢 FIXED: Match the expected type structure used by your filter class
    @GetMapping("/api/v1/auth/validate")
    AuthResponse validateToken(@RequestParam("token") String token);
}