package com.prevpaper.content.service.Impl;

import com.prevpaper.comman.dto.FileTaskEvent;
import com.prevpaper.comman.dto.UploadResultDTO;
import com.prevpaper.content.dto.ContentUploadRequest;
import com.prevpaper.content.entities.Content;
import com.prevpaper.content.enums.VerificationStatus;
import com.prevpaper.content.repository.ContentRepository;
import com.prevpaper.content.service.ContentService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContentServiceImpl implements ContentService {

    private final ContentRepository contentRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.upload-task}")
    private String uploadTaskTopic;

    @Override
    @Transactional
    public Content initiateUpload(ContentUploadRequest request, MultipartFile file, UUID userId) {
        // 1. Map DTO to Entity using Builder Pattern
        Content content = Content.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .contentType(request.getContentType())
                .universityId(request.getUniversityId())
                .departmentId(request.getDepartmentId())
                .programId(request.getProgramId())
                .academicYear(request.getAcademicYear())
                .semester(request.getSemester())
                .subjectId(request.getSubjectId())
                .uploadedBy(userId)
                .fileType(request.getFileType())
                .verificationStatus(VerificationStatus.PENDING_UPLOAD)
                .fileUrl(null) // Step 3: URL is NULL [cite: 23]
                .build();

        // 2. Step 3: Insert metadata into DB
        Content savedContent = contentRepository.save(content);

        // 3. Step 4: Emit Kafka Event with File Bytes and Content ID
        try {
            // Note: In production, consider temporary storage if files are huge (>10MB)
            FileTaskEvent event = new FileTaskEvent(savedContent.getId(), file.getBytes(), file.getOriginalFilename());
            kafkaTemplate.send(uploadTaskTopic, savedContent.getId().toString(), event);
            log.info("Emitted upload task for Content ID: {}", savedContent.getId());
        } catch (IOException e) {
            throw new RuntimeException("Failed to process file bytes for Kafka", e);
        }

        return savedContent;
    }

    @Override
    @Transactional
    public void finalizeUploadStatus(UUID contentId, UploadResultDTO result) {
        // 1. Fetch the record we created in Step 3
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new RuntimeException("Content not found for ID: " + contentId));

        if (result.isSuccess()) {
            // 2. SUCCESS: Update URL and set status for Admin Review
            content.setFileUrl(result.getFileUrl());
            content.setVerificationStatus(VerificationStatus.PENDING);
            log.info("✅ Content ID {} is now PENDING review with URL: {}", contentId, result.getFileUrl());

            // 3. Step 7: Trigger Notification to Admin/User
            // notificationProducer.sendUploadSuccessEvent(content);

        } else {
            // 4. FAILURE: Mark as rejected and log the error from Upload Service
            content.setVerificationStatus(VerificationStatus.REJECTED);
            log.error("❌ Content ID {} upload failed: {}", contentId, result.getErrorMessage());

            // notificationProducer.sendUploadFailureEvent(content, result.getErrorMessage());
        }

        contentRepository.save(content);
    }
}