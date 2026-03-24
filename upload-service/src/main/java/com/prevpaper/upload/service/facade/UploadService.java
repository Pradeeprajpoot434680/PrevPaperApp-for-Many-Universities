package com.prevpaper.upload.service.facade;

import com.prevpaper.upload.dto.FileMetadata;
import com.prevpaper.upload.enums.FileType;
import com.prevpaper.upload.service.factory.FileHandlerFactory;
import com.prevpaper.upload.service.storage.StorageService;
import com.prevpaper.upload.service.strategy.FileProcessingStrategy;
import com.prevpaper.upload.observer.UploadObserver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UploadService {

    private final FileHandlerFactory handlerFactory;
    private final StorageService storageService; // Injected as Cloudinary due to @Primary
    private final List<UploadObserver> observers; // Automatically gets KafkaUploadObserver

    /**
     * This is the Facade method.
     * The Content Service just calls this and everything else happens internally.
     */
    public FileMetadata uploadFile(MultipartFile file, UUID userId) {
        // 1. Resolve File Type (e.g., "pdf" -> FileType.PDF)
        String extension = getFileExtension(file.getOriginalFilename());
        FileType type = handlerFactory.resolveType(extension);

        // 2. Factory: Get Strategy and Process (Validation)
        FileProcessingStrategy strategy = handlerFactory.getStrategy(type);
        strategy.process(file);

        // 3. Storage: Upload to Cloud [cite: 31-33]
        FileMetadata metadata = storageService.store(file, userId);

        // 4. Observer: Notify Kafka [cite: 4]
        notifyObservers(metadata);

        return metadata;
    }

    private void notifyObservers(FileMetadata metadata) {
        observers.forEach(observer -> observer.onUploadSuccess(metadata));
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) return "";
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }
}