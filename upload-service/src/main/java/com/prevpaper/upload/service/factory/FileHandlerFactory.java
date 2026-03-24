package com.prevpaper.upload.service.factory;

import com.prevpaper.upload.enums.FileType;
import com.prevpaper.upload.service.strategy.FileProcessingStrategy;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class FileHandlerFactory {

    // A Map to hold all our strategies: Key = FileType, Value = Strategy Instance
    private final Map<FileType, FileProcessingStrategy> strategies = new HashMap<>();

    /**
     * Spring will automatically inject all classes that implement
     * FileProcessingStrategy into this constructor.
     */
    public FileHandlerFactory(List<FileProcessingStrategy> strategyList) {
        strategyList.forEach(strategy ->
                strategies.put(strategy.getSupportedType(), strategy)
        );
    }

    /**
     * The core Factory method to get the correct strategy.
     */
    public FileProcessingStrategy getStrategy(FileType type) {
        return Optional.ofNullable(strategies.get(type))
                .orElseThrow(() -> new RuntimeException("No processing strategy found for type: " + type));
    }

    /**
     * Helper to resolve FileType from an extension string (e.g., "pdf" -> FileType.PDF)
     */
    public FileType resolveType(String extension) {
        try {
            return FileType.valueOf(extension.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Unsupported file extension: " + extension);
        }
    }
}