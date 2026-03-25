package com.prevpaper.upload.client;

import com.prevpaper.comman.dto.UploadResultDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(name = "CONTENT-SERVICE")
public interface ContentServiceClient {
    @PatchMapping("/api/v1/content/internal/status/{contentId}")
    void updateUploadStatus(@PathVariable UUID contentId, @RequestBody UploadResultDTO result);
}