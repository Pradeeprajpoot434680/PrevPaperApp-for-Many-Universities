package com.prevpaper.university.client;

import com.prevpaper.comman.dto.UserBatchRequest;
import com.prevpaper.comman.dto.UserDetailDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "AUTH-SERVICE")
public interface AuthClient {

    @PostMapping("/api/v1/auth/internal/users/details-batch")
    List<UserDetailDTO> getUserDetailsBatch(@RequestBody UserBatchRequest request);
}
