package com.prevpaper.university.client;

import com.prevpaper.comman.dto.ContentStatsDTO;
import com.prevpaper.comman.dto.PendingContentDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "CONTENT-SERVICE")
public interface ContentClient {
    @GetMapping("/api/v1/content/internal/stats/semester")
    ContentStatsDTO getSemesterStats(
            @RequestParam UUID programId,
            @RequestParam Integer semester
    );

    @GetMapping("/api/v1/content/internal/pending/{scopeId}")
    List<PendingContentDTO> getPendingByScope(@PathVariable UUID scopeId);

    @PutMapping("/api/v1/content/internal/{contentId}/status")
    void updateStatus(
            @PathVariable UUID contentId,
            @RequestParam String status,
            @RequestParam UUID verifiedBy
    );

    @DeleteMapping("/api/v1/content/internal/{contentId}")
    void deleteById(@PathVariable UUID contentId);
}