package com.prevpaper.upload.service.strategy.Impl;

import com.prevpaper.upload.enums.FileType;
import com.prevpaper.upload.service.strategy.FileProcessingStrategy;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import java.util.Arrays;
import java.util.List;

@Component
public class ImageProcessingStrategy implements FileProcessingStrategy {

    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024; // 5MB limit for images
    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList("image/png", "image/jpeg", "image/jpg");

    @Override
    public void process(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Image file is empty.");
        }

        // 1. Size Check (Images are usually smaller than PDFs)
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new RuntimeException("Image exceeds 5MB limit.");
        }

        // 2. MIME Type Check
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new RuntimeException("Invalid image format. Supported: PNG, JPG, JPEG");
        }
    }

    @Override
    public FileType getSupportedType() {
        return FileType.IMAGE;
    }
}