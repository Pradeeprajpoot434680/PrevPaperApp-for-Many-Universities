package com.prevpaper.content.controller;

import com.prevpaper.comman.dto.UploadResultDTO;
import com.prevpaper.content.dto.ContentUploadRequest;
import com.prevpaper.content.entities.Content;
import com.prevpaper.content.service.ContentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
}