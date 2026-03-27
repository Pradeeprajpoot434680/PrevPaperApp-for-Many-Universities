package com.prevpaper.content.client;

import com.prevpaper.comman.dto.UserInternalInfoDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "AUTH-SERVICE")
public interface AuthServiceClient {
    @GetMapping("/api/v1/auth/internal/user/{id}")
    UserInternalInfoDTO getUserDetails(@PathVariable("id") UUID id);
}
