package com.prevpaper.content.controller;

import com.prevpaper.comman.dto.PendingContentDTO;
import com.prevpaper.comman.dto.UploadResultDTO;
import com.prevpaper.content.dto.ContentSearchRequest;
import com.prevpaper.content.dto.ContentSearchResponseDTO;
import com.prevpaper.content.dto.ContentUploadRequest;
import com.prevpaper.content.entities.Content;
import com.prevpaper.content.enums.VerificationStatus;
import com.prevpaper.content.service.ContentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/content")
public class ContentController {

    private final ContentService contentService;

    public ContentController(ContentService contentService) {
        this.contentService = contentService;
    }

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<Content> uploadContent(
            @RequestPart("metadata") @Valid ContentUploadRequest request,
            @RequestPart("file") MultipartFile file,
            @RequestHeader("X-User-Id") String userId) {

        // Step 2 & 3: Delegate to service to save metadata and emit Kafka task
        Content savedContent = contentService.initiateUpload(
                request,
                file,
                UUID.fromString(userId)
        );

        return ResponseEntity.accepted().body(savedContent); // 202 Accepted
    }

    @PatchMapping("/internal/status/{contentId}")
    public ResponseEntity<Void> updateUploadStatus(
            @PathVariable UUID contentId,
            @RequestBody UploadResultDTO result) {

        contentService.finalizeUploadStatus(contentId, result);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/internal/pending/session")
    public List<PendingContentDTO> getPendingBySession(
            @RequestParam UUID programId,
            @RequestParam Integer academicYear) {
        // Pass the parameters to the service layer
        return contentService.getPendingContent(programId, academicYear, VerificationStatus.PENDING);
    }

    @PostMapping("/search")
    public ResponseEntity<List<ContentSearchResponseDTO>> searchContent(@RequestBody ContentSearchRequest request) {
        // Returns clean, compile-safe, non-entity lists
        return ResponseEntity.ok(contentService.search(request));
    }
}