package com.prevpaper.upload.service.storage.Impl;


import com.prevpaper.upload.dto.FileMetadata;
import com.prevpaper.upload.enums.FileType;
import com.prevpaper.upload.service.storage.StorageService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class LocalStorageService implements StorageService {

    private final Path rootPath = Paths.get("uploads");

    public LocalStorageService() {
        try {
            Files.createDirectories(rootPath);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage folder", e);
        }
    }

    @Override
    public FileMetadata store(MultipartFile file, UUID userId) {
        String originalFileName = file.getOriginalFilename();
        String fileExtension = originalFileName.substring(originalFileName.lastIndexOf(".") + 1);

        // Generate a unique path to prevent overwriting
        String storageName = UUID.randomUUID() + "_" + originalFileName;
        Path targetLocation = rootPath.resolve(storageName);

        try {
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // Return the Metadata "Receipt" [cite: 38-46]
            return FileMetadata.builder()
                    .id(UUID.randomUUID()) // [cite: 39]
                    .fileName(originalFileName) // [cite: 40]
                    .fileType(FileType.valueOf(fileExtension.toUpperCase())) // [cite: 41]
                    .fileSize(file.getSize()) // [cite: 42]
                    .storagePath(targetLocation.toString()) // [cite: 43]
                    .fileUrl("http://localhost:8084/files/" + storageName) // [cite: 44]
                    .uploadedBy(userId) // [cite: 45]
                    .createdAt(LocalDateTime.now()) // [cite: 46]
                    .build();

        } catch (IOException e) {
            throw new RuntimeException("Failed to store file locally", e);
        }
    }

    @Override
    public void delete(String storagePath) {
        try {
            Files.deleteIfExists(Paths.get(storagePath));
        } catch (IOException e) {
            // Log warning but don't stop execution
        }
    }
}