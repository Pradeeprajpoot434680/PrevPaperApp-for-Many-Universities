package com.prevpaper.upload.dto;

import com.prevpaper.upload.enums.FileType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class FileMetadata {
    private UUID id;
    private String fileName;    // Original name (e.g., "math_midsem.pdf")
    private FileType fileType;
    private Long fileSize;      // Size in bytes for validation
    private String storagePath; // The internal path (e.g., "/s3/bucket/path")
    private String fileUrl;     // The public/pre-signed URL for the user
    private UUID uploadedBy;    // User ID from the X-User-Id header
    private LocalDateTime createdAt; // Audit timestamp
}