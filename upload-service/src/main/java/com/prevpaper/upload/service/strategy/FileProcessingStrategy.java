package com.prevpaper.upload.service.strategy;

import org.springframework.web.multipart.MultipartFile;
import com.prevpaper.upload.enums.FileType;


public interface FileProcessingStrategy {

    /**
     * Performs file-specific validation (size, extension, corruption check).
     * @param file The raw multipart file from the request.
     * @throws RuntimeException if validation fails.
     */
    void process(MultipartFile file);

    /**
     * Tells the Factory which FileType this strategy handles.
     * @return The supported FileType (PDF, TXT, etc.)
     */
    FileType getSupportedType();
}