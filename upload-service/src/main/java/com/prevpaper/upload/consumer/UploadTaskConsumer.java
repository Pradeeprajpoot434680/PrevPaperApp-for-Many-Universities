package com.prevpaper.upload.consumer;

import com.prevpaper.comman.dto.FileTaskEvent;
import com.prevpaper.comman.dto.UploadResultDTO;
import com.prevpaper.upload.client.ContentServiceClient;
import com.prevpaper.upload.dto.FileMetadata;
import com.prevpaper.upload.service.facade.UploadService;
import com.prevpaper.upload.utils.InMemoryMultipartFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UploadTaskConsumer {

    private final UploadService uploadService;
    private final ContentServiceClient contentClient;

    @KafkaListener(
            topics = "${app.kafka.topics.upload-task}",
            groupId = "upload-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeUploadTask(FileTaskEvent event) {
        if (event == null || event.getContentId() == null) {
            log.error("Received null or invalid FileTaskEvent");
            return;
        }

        log.info("Received async upload task for Content ID: {}, FileName: {}",
                event.getContentId(), event.getFileName());

        try {
            // 1. Convert byte payload back to MultipartFile
            MultipartFile file = new InMemoryMultipartFile(event.getFileBytes(), event.getFileName());

            // 2. Delegate file storage to Cloudinary via UploadService
            FileMetadata metadata = uploadService.uploadFile(file, UUID.randomUUID());

            // 3. Construct success status payload
            UploadResultDTO result = UploadResultDTO.builder()
                    .success(true)
                    .fileUrl(metadata.getFileUrl())
                    .build();

            // 4. Update Content Service status via Feign Client
            contentClient.updateUploadStatus(event.getContentId(), result);
            log.info("Successfully uploaded file and updated Content Service for Content ID: {}", event.getContentId());

        } catch (Exception e) {
            log.error("Failed to process upload task for Content ID: {}. Error: {}",
                    event.getContentId(), e.getMessage(), e);

            // Failure Callback to update status in Content Service
            UploadResultDTO errorResult = UploadResultDTO.builder()
                    .success(false)
                    .errorMessage(e.getMessage() != null ? e.getMessage() : "Unknown upload processing failure")
                    .build();

            try {
                contentClient.updateUploadStatus(event.getContentId(), errorResult);
                log.info("Notified Content Service of upload failure for Content ID: {}", event.getContentId());
            } catch (Exception feignException) {
                log.error("Critical: Could not notify Content Service of failure for Content ID: {}. Feign Error: {}",
                        event.getContentId(), feignException.getMessage(), feignException);
            }
        }
    }
}