package com.prevpaper.university.client;

import com.prevpaper.comman.dto.ContentStatsDTO;
import com.prevpaper.comman.dto.PendingContentDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

// 🟢 FIXED: Injected explicit url parameter with environment fallback for ContentClient
@FeignClient(
        name = "CONTENT-SERVICE",
        url = "${CONTENT_SERVICE_URL:http://content-service:8090}"
)
public interface ContentClient {

    @GetMapping("/api/v1/content/internal/stats/semester")
    ContentStatsDTO getSemesterStats(
            @RequestParam UUID programId,
            @RequestParam Integer semester
    );

    @GetMapping("/api/v1/content/internal/pending/{scopeId}")
    List<PendingContentDTO> getPendingByScope(@PathVariable UUID scopeId);

    @GetMapping("/api/v1/content/internal/pending/session")
    // 🟢 COHORT-BASED: batchStartYear is the SessionRep's assigned batch (e.g., Batch 2023)
    // The content service uses formula: batchStartYear = academicYear - FLOOR((semester - 1) / 2)
    // to return only papers belonging to this batch's academic progression.
    List<PendingContentDTO> getPendingBySession(
            @RequestParam("programId") UUID programId,
            @RequestParam("batchStartYear") Integer batchStartYear
    );

    @PutMapping("/api/v1/content/internal/{contentId}/status")
    void updateStatus(
            @PathVariable UUID contentId,
            @RequestParam String status,
            @RequestParam UUID verifiedBy
    );

    @DeleteMapping("/api/v1/content/internal/{contentId}")
    void deleteById(@PathVariable UUID contentId);
}