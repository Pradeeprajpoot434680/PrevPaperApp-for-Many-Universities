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
            containerFactory = "kafkaListenerContainerFactory" // Ensure JSON deserialization is used
    )
    public void consumeUploadTask(FileTaskEvent event) {
        log.info("Received upload task for Content ID: {}", event.getContentId());

        try {
            // 1. Convert bytes to MultipartFile
            MultipartFile file = new InMemoryMultipartFile(event.getFileBytes(), event.getFileName());

            // 2. Process via Facade (Factory -> Strategy -> Storage)
            // Passing a System UUID or extracting from event if available
            FileMetadata metadata = uploadService.uploadFile(file, UUID.randomUUID());

            // 3. Step 6: Build Success Result
            UploadResultDTO result = UploadResultDTO.builder()
                    .success(true)
                    .fileUrl(metadata.getFileUrl())
                    .build();

            // 4. Update Content Service via Feign
            contentClient.updateUploadStatus(event.getContentId(), result);
            log.info("Successfully updated Content Service for ID: {}", event.getContentId());

        } catch (Exception e) {
            log.error("Failed to process upload for Content ID: {}. Error: {}", event.getContentId(), e.getMessage());

            // Step 6: Failure Callback to notify Content Service of the error
            UploadResultDTO errorResult = UploadResultDTO.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();

            try {
                contentClient.updateUploadStatus(event.getContentId(), errorResult);
            } catch (Exception feignException) {
                log.error("Critial: Could not notify Content Service of failure for ID: {}", event.getContentId());
            }
        }
    }
}