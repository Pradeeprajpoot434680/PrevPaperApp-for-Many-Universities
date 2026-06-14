package com.prevpaper.comman.producer; // Note: Consider renaming package "comman" to "common" later!

import com.prevpaper.comman.dto.FileTaskEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
public class ContentProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String uploadTaskTopic;

    // Standard constructor injection manages both the template and the config parameter cleanly
    public ContentProducer(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${app.kafka.topics.upload-task:default-upload-topic}") String uploadTaskTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.uploadTaskTopic = uploadTaskTopic;
    }

    public void sendUploadTask(UUID contentId, MultipartFile file) {
        try {
            FileTaskEvent event = new FileTaskEvent();
            event.setContentId(contentId);
            event.setFileBytes(file.getBytes());
            event.setFileName(file.getOriginalFilename());

            kafkaTemplate.send(uploadTaskTopic, contentId.toString(), event);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file for Kafka transmission", e);
        }
    }
}