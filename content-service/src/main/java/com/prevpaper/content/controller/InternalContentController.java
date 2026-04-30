package com.prevpaper.content.controller;

import com.prevpaper.comman.dto.ContentStatsDTO;
import com.prevpaper.comman.dto.PendingContentDTO;
import com.prevpaper.content.dto.ContentTypeCountDTO;
import com.prevpaper.content.dto.UniversityContentSummaryDTO;
import com.prevpaper.content.enums.VerificationStatus;
import com.prevpaper.content.service.ContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/content/internal")
@RequiredArgsConstructor
public class InternalContentController {

    private final ContentService contentService;

    @GetMapping("/stats/semester")
    public ContentStatsDTO getSemesterStats(
            @RequestParam UUID programId,
            @RequestParam Integer semester) {

        return contentService.getStatsByProgramAndSemester(programId, semester);
    }

    @GetMapping("/stats/university/{universityId}")
    public ResponseEntity<UniversityContentSummaryDTO> getUniversityContentStats(
            @PathVariable UUID universityId) {


        UniversityContentSummaryDTO universityContentSummaryDTO = contentService.countContentGroupedByType(universityId);



        return ResponseEntity.ok(universityContentSummaryDTO);
    }

    @GetMapping("/pending/{scopeId}")
    public List<PendingContentDTO> getPendingByScope(@PathVariable UUID scopeId) {
        // This query matches scopeId against University, Department, or Program
        return contentService.findPendingContent(scopeId);
    }
}