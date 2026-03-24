package com.prevpaper.auth.client;


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "UNIVERSITY-SERVICE")
public interface UniversityClient {

    @GetMapping("/api/v1/university-rep/{universityId}/exists")
    Boolean checkUniversityExists(@PathVariable("universityId") UUID universityId);

}
