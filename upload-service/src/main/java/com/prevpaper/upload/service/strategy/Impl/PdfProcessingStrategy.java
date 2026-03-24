package com.prevpaper.upload.service.strategy.Impl;

import com.prevpaper.upload.service.strategy.FileProcessingStrategy;


import com.prevpaper.upload.enums.FileType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class PdfProcessingStrategy implements FileProcessingStrategy {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB limit

    @Override
    public void process(MultipartFile file) {
        // 1. Basic Null/Empty Check
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File is empty or null.");
        }

        // 2. Size Validation
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new RuntimeException("File size exceeds 10MB limit.");
        }

        // 3. Content Type Validation
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equalsIgnoreCase("application/pdf")) {
            throw new RuntimeException("Invalid file format. Only PDF is allowed for this strategy.");
        }

        // Note: You could add deep-scan logic here (e.g., using Apache PDFBox
        // to check if the PDF is password protected).
    }

    @Override
    public FileType getSupportedType() {
        return FileType.PDF;
    }
}