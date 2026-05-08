package com.prevpaper.upload.service.storage.Impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.prevpaper.upload.dto.FileMetadata;
import com.prevpaper.upload.enums.FileType;
import com.prevpaper.upload.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@Primary
@RequiredArgsConstructor
@Slf4j
public class CloudinaryStorageService implements StorageService {

    private final Cloudinary cloudinary;

    @Override
    public FileMetadata store(MultipartFile file, UUID userId) {
        try {
            // 1. Determine resource type
            // PDFs often need 'raw' or 'auto' with explicit flags to avoid corruption
            String originalFilename = file.getOriginalFilename();
            String resourceType = (originalFilename != null && originalFilename.toLowerCase().endsWith(".pdf"))
                    ? "auto" : "image";

            log.info("Uploading file {} as resource_type: {}", originalFilename, resourceType);

            // 2. Upload to Cloudinary with explicit options
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap(
                            "resource_type", resourceType,
                            "folder", "university_content",
                            "access_mode", "public" // Ensures the secure_url is accessible
                    ));

            // 3. Extract Metadata
            String publicId = (String) uploadResult.get("public_id");
            String url = (String) uploadResult.get("secure_url");

            // Handle cases where bytes might be returned as Integer or Long
            Long size = Long.valueOf(uploadResult.get("bytes").toString());

            return FileMetadata.builder()
                    .id(UUID.randomUUID())
                    .fileName(originalFilename)
                    .fileType(resolveFileType(originalFilename))
                    .fileSize(size)
                    .storagePath(publicId)
                    .fileUrl(url)
                    .uploadedBy(userId)
                    .createdAt(LocalDateTime.now())
                    .build();

        } catch (IOException e) {
            log.error("Cloudinary I/O error for user {}: {}", userId, e.getMessage());
            throw new RuntimeException("Cloudinary upload failed", e);
        }
    }

    @Override
    public void delete(String storagePath) {
        try {
            // Use 'auto' to ensure it finds the correct resource type to delete
            cloudinary.uploader().destroy(storagePath, ObjectUtils.asMap("resource_type", "auto"));
        } catch (IOException e) {
            log.error("Failed to delete Cloudinary asset at {}: {}", storagePath, e.getMessage());
        }
    }

    private FileType resolveFileType(String fileName) {
        if (fileName != null && fileName.toLowerCase().endsWith(".pdf")) return FileType.PDF;
        return FileType.IMAGE;
    }
}