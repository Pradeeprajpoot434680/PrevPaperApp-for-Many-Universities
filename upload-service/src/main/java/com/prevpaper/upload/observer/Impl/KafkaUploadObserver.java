package com.prevpaper.upload.observer.Impl;


import com.prevpaper.upload.dto.FileMetadata;
import com.prevpaper.upload.observer.UploadObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class KafkaUploadObserver implements UploadObserver {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC = "file-upload-events";

    public KafkaUploadObserver(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void onUploadSuccess(FileMetadata metadata) {
        log.info("Emitting Kafka event for file: {}", metadata.getFileName());
        // This sends the "Where" (URL/Path) so the Content Service can add the "What"
        kafkaTemplate.send(TOPIC, metadata.getId().toString(), metadata);
    }
}
