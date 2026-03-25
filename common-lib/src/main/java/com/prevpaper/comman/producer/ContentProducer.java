package com.prevpaper.comman.producer;

import com.prevpaper.comman.dto.FileTaskEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContentProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    @Value("${app.kafka.topics.upload-task}")
    private String uploadTaskTopic;

    public void sendUploadTask(UUID contentId, MultipartFile file) {
        try {
            // Wrap the data in our "Envelope" DTO
            FileTaskEvent event = new FileTaskEvent();
            event.setContentId(contentId);
            event.setFileBytes(file.getBytes());
            event.setFileName(file.getOriginalFilename());

            // Send to Kafka
            kafkaTemplate.send(uploadTaskTopic, contentId.toString(), event);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file for Kafka transmission", e);
        }
    }
}
