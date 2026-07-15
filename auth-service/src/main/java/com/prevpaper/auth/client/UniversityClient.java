package com.prevpaper.auth.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.UUID;

// 🟢 FIXED: Fallback configuration structure uses property mappings, or directly targets the Docker host network container.
@FeignClient(
        name = "UNIVERSITY-SERVICE",
        url = "${feign.client.config.university-service.url:http://university-service:8082}"
)
public interface UniversityClient {

    @GetMapping("/api/v1/university-rep/{universityId}/exists")
    Boolean checkUniversityExists(@PathVariable("universityId") UUID universityId);
}