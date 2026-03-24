package com.prevpaper.upload.service.storage.Impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.prevpaper.upload.dto.FileMetadata;
import com.prevpaper.upload.enums.FileType;
import com.prevpaper.upload.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@Primary // Tells Spring to use Cloudinary instead of Local by default
@RequiredArgsConstructor
public class CloudinaryStorageService implements StorageService {

    private final Cloudinary cloudinary;

    @Override
    public FileMetadata store(MultipartFile file, UUID userId) {
        try {
            // 1. Upload to Cloudinary
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap("resource_type", "auto"));

            // 2. Extract Cloud Metadata [cite: 40, 42, 44]
            String publicId = (String) uploadResult.get("public_id");
            String url = (String) uploadResult.get("secure_url");
            Long size = Long.valueOf(uploadResult.get("bytes").toString());

            // 3. Map to our standard FileMetadata DTO
            return FileMetadata.builder()
                    .id(UUID.randomUUID())
                    .fileName(file.getOriginalFilename())
                    .fileType(resolveFileType(file.getOriginalFilename()))
                    .fileSize(size)
                    .storagePath(publicId) // Used for deletion later
                    .fileUrl(url)
                    .uploadedBy(userId)
                    .createdAt(LocalDateTime.now())
                    .build();

        } catch (IOException e) {
            throw new RuntimeException("Cloudinary upload failed", e);
        }
    }

    @Override
    public void delete(String storagePath) {
        try {
            cloudinary.uploader().destroy(storagePath, ObjectUtils.emptyMap());
        } catch (IOException e) {
            // Log error
        }
    }

    private FileType resolveFileType(String fileName) {
        if (fileName.toLowerCase().endsWith(".pdf")) return FileType.PDF;
        return FileType.IMAGE;
    }
}