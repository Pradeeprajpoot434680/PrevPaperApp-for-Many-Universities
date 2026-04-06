package com.prevpaper.gateway.client;

import com.prevpaper.comman.dto.AuthResponse;
import com.prevpaper.gateway.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
@FeignClient(name = "AUTH-SERVICE", configuration = FeignConfig.class)
public interface AuthClient {
    @GetMapping("/api/v1/auth/validate")
    AuthResponse validateToken(@RequestParam("token") String token);
}
