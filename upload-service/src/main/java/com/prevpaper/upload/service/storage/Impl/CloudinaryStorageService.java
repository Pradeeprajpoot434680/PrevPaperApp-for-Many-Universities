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
            String originalFilename = file.getOriginalFilename();
            String lowerName = originalFilename != null ? originalFilename.toLowerCase() : "";

            // 🟢 Handle PDFs as 'raw' or 'auto' to preserve PDF structure, images as 'auto'
            String resourceType = lowerName.endsWith(".pdf") ? "raw" : "auto";

            log.info("Uploading file '{}' as resource_type: {}", originalFilename, resourceType);

            // Upload to Cloudinary
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap(
                            "resource_type", resourceType,
                            "folder", "university_content",
                            "access_mode", "public"
                    ));

            String publicId = (String) uploadResult.get("public_id");
            String url = (String) uploadResult.get("secure_url");

            // Extract file size safely regardless of return type (Integer/Long)
            Object bytesObj = uploadResult.get("bytes");
            Long size = bytesObj != null ? Long.valueOf(bytesObj.toString()) : file.getSize();

            log.info("Cloudinary upload successful. Public ID: {}, URL: {}", publicId, url);

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
            log.error("Cloudinary I/O error during upload for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Cloudinary upload failed: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error during Cloudinary upload for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Storage processing failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) return;

        try {
            cloudinary.uploader().destroy(storagePath, ObjectUtils.asMap("resource_type", "auto"));
            log.info("Successfully deleted Cloudinary asset at path: {}", storagePath);
        } catch (IOException e) {
            log.error("Failed to delete Cloudinary asset at {}: {}", storagePath, e.getMessage(), e);
        }
    }

    private FileType resolveFileType(String fileName) {
        if (fileName != null && fileName.toLowerCase().endsWith(".pdf")) {
            return FileType.PDF;
        }
        return FileType.IMAGE;
    }
}