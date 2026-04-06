package com.prevpaper.university.client;

import com.prevpaper.comman.dto.UserBatchRequest;
import com.prevpaper.comman.dto.UserDetailDTO;
import com.prevpaper.comman.dto.UserInternalInfoDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "AUTH-SERVICE")
public interface AuthClient {

    @PostMapping("/api/v1/auth/internal/users/details-batch")
    List<UserDetailDTO> getUserDetailsBatch(@RequestBody UserBatchRequest request);

    @GetMapping("/api/v1/auth/internal/user/{id}")
    UserInternalInfoDTO getAuthUserInfo(@PathVariable("id") UUID id);

}
