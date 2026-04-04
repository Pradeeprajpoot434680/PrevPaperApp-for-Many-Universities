package com.prevpaper.user.client;

import com.prevpaper.comman.dto.UserInternalInfoDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "AUTH-SERVICE")
public interface AuthServiceClient {
    @GetMapping("/api/v1/auth/internal/user/{id}")
    UserInternalInfoDTO getAuthUserInfo(@PathVariable("id") UUID id);

    @PostMapping("/api/v1/auth/internal/user/set-fullName")
    void setFullName(@RequestParam("authId") UUID authId,
                     @RequestParam("fullName") String fullName);
}