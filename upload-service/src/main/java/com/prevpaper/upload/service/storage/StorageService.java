
package com.prevpaper.upload.service.storage;

import com.prevpaper.upload.dto.FileMetadata;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;

public interface StorageService {
    /**
     * Stores the file and returns the metadata (URL, Path, Size).
     */
    FileMetadata store(MultipartFile file, UUID userId);

    /**
     * Deletes a file from storage if the upload transaction fails.
     */
    void delete(String storagePath);
}