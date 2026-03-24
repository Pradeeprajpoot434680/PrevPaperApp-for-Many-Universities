package com.prevpaper.upload.controller;

import com.prevpaper.upload.dto.FileMetadata;
import com.prevpaper.upload.service.facade.UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/upload")
public class UploadController {

    private final UploadService uploadService;

    public UploadController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    @PostMapping
    public ResponseEntity<FileMetadata> upload(
            @RequestParam("file") MultipartFile file,
            @RequestHeader("X-User-Id") String userId) {

        FileMetadata metadata = uploadService.uploadFile(file, UUID.fromString(userId));
        return ResponseEntity.ok(metadata);
    }
}
