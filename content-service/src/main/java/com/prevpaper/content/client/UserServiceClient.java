package com.prevpaper.content.client;

import com.prevpaper.comman.dto.UserInternalDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(
        name = "USER-SERVICE",
        url = "${USER_SERVICE_URL:http://user-service:8086}"
)
public interface UserServiceClient {
    @GetMapping("/api/v1/users/internal/{userId}")
    UserInternalDTO getUserInternalInfo(@PathVariable UUID userId);
}

