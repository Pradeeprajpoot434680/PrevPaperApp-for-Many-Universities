package com.prevpaper.content.service.Impl;

import com.prevpaper.comman.dto.*;
import com.prevpaper.comman.enums.NotificationEventType;
import com.prevpaper.comman.enums.NotificationType;
import com.prevpaper.comman.exception.ContentAlreadyExist;
import com.prevpaper.comman.producer.NotificationProducer;
import com.prevpaper.content.client.AuthServiceClient;
import com.prevpaper.content.client.UserServiceClient;
import com.prevpaper.content.dto.ContentSearchRequest;
import com.prevpaper.content.dto.ContentTypeCountDTO;
import com.prevpaper.content.dto.ContentUploadRequest;
import com.prevpaper.content.dto.UniversityContentSummaryDTO;
import com.prevpaper.content.entities.Content;
import com.prevpaper.content.enums.VerificationStatus;
import com.prevpaper.content.repository.ContentRepository;
import com.prevpaper.content.repository.ContentSpecifications;
import com.prevpaper.content.service.ContentService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.HtmlUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContentServiceImpl implements ContentService {

    private final ContentRepository contentRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private  final NotificationProducer notificationProducer;
    private  final AuthServiceClient authServiceClient;
    private final UserServiceClient userServiceClient;
    @Value("${app.kafka.topics.upload-task}")
    private String uploadTaskTopic;

    @Override
    @Transactional
    public Content initiateUpload(ContentUploadRequest request, MultipartFile file, UUID userId) {
        log.info("Content upload request received: uploaderId={}, universityId={}, departmentId={}, programId={}, semester={}, subjectId={}, contentType={}, fileType={}, originalFileName={}, fileSizeBytes={}",
                userId,
                request.getUniversityId(),
                request.getDepartmentId(),
                request.getProgramId(),
                request.getSemester(),
                request.getSubjectId(),
                request.getContentType(),
                request.getFileType(),
                file != null ? file.getOriginalFilename() : null,
                file != null ? file.getSize() : null);

        // 1. DUPLICATE CHECK: Prevent re-uploading existing content
        boolean exists = contentRepository.existsByUniversityIdAndDepartmentIdAndProgramIdAndSemesterAndSubjectId(
                request.getUniversityId(),
                request.getDepartmentId(),
                request.getProgramId(),
                request.getSemester(),
                request.getSubjectId()
        );

        if (exists) {
            log.warn("Content upload rejected: duplicate content, uploaderId={}, universityId={}, departmentId={}, programId={}, semester={}, subjectId={}",
                    userId, request.getUniversityId(), request.getDepartmentId(), request.getProgramId(),
                    request.getSemester(), request.getSubjectId());
            throw new ContentAlreadyExist("Content already exists for this university, program, and subject.");
        }


        if (file == null || file.isEmpty()) {
            log.warn("Content upload rejected: missing or empty file, uploaderId={}, subjectId={}, originalFileName={}",
                    userId, request.getSubjectId(), file != null ? file.getOriginalFilename() : null);
            throw new RuntimeException("File is missing or empty.");
        }

        // 1. Read bytes once to avoid multiple stream consumption issues
        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            log.error("Content upload failed while reading file bytes: uploaderId={}, subjectId={}, originalFileName={}, error={}",
                    userId, request.getSubjectId(), file.getOriginalFilename(), e.getMessage(), e);
            throw new RuntimeException("Could not process file bytes");
        }
        log.info("Content upload file bytes loaded: uploaderId={}, subjectId={}, originalFileName={}, fileSizeBytes={}",
                userId, request.getSubjectId(), file.getOriginalFilename(), fileBytes.length);

        // 2. Security Validation (Type & Size)
        validateFile(fileBytes, file.getOriginalFilename(), userId, request.getSubjectId());

        // 3. Mandatory Antivirus Scan
        scanForVirus(fileBytes, file.getOriginalFilename(), userId, request.getSubjectId());

        // 4. Sanitize Input to prevent XSS
        String safeTitle = HtmlUtils.htmlEscape(request.getTitle());
        String safeDescription = request.getDescription() != null
                ? HtmlUtils.htmlEscape(request.getDescription())
                : "";

        // 5. Save Metadata with PENDING_UPLOAD status
        Content content = Content.builder()
                .title(safeTitle)
                .description(safeDescription)
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
                .fileUrl(null)
                .build();

        Content savedContent = contentRepository.save(content);
        log.info("Content metadata saved: contentId={}, uploaderId={}, universityId={}, departmentId={}, programId={}, semester={}, subjectId={}, status={}",
                savedContent.getId(), userId, savedContent.getUniversityId(), savedContent.getDepartmentId(),
                savedContent.getProgramId(), savedContent.getSemester(), savedContent.getSubjectId(),
                savedContent.getVerificationStatus());

        // 6. Emit Kafka Event with sanitized filename
        String secureFileName = sanitizeFileName(file.getOriginalFilename());
        FileTaskEvent event = new FileTaskEvent(savedContent.getId(), fileBytes, secureFileName);
        log.info("Content upload task prepared: contentId={}, uploaderId={}, sanitizedFileName={}, topic={}",
                savedContent.getId(), userId, secureFileName, uploadTaskTopic);

        try {
            kafkaTemplate.send(uploadTaskTopic, savedContent.getId().toString(), event);
            log.info("Content upload task emitted: contentId={}, uploaderId={}, topic={}",
                    savedContent.getId(), userId, uploadTaskTopic);
        } catch (Exception e) {
            log.error("Content upload task emission failed: contentId={}, uploaderId={}, topic={}, error={}",
                    savedContent.getId(), userId, uploadTaskTopic, e.getMessage(), e);
            throw new RuntimeException("System error during upload queuing");
        }

        return savedContent;
    }

    private void validateFile(byte[] fileBytes, String originalFileName, UUID userId, UUID subjectId) {
        // A. Size Validation (e.g., 15MB)
        if (fileBytes.length > 15 * 1024 * 1024) {
            log.warn("Content upload rejected: file too large, uploaderId={}, subjectId={}, originalFileName={}, fileSizeBytes={}",
                    userId, subjectId, originalFileName, fileBytes.length);
            throw new RuntimeException("File too large (Max 15MB allowed)");
        }

        // B. Magic Byte Detection (Apache Tika)
        Tika tika = new Tika();
        String detectedType = tika.detect(fileBytes);

        List<String> allowedTypes = List.of("application/pdf", "image/jpeg", "image/png");

        if (!allowedTypes.contains(detectedType)) {
            log.error("Content upload rejected: unauthorized file signature, uploaderId={}, subjectId={}, originalFileName={}, detectedType={}",
                    userId, subjectId, originalFileName, detectedType);
            throw new RuntimeException("Invalid file format: Detected " + detectedType);
        }
        log.info("Content upload file validation passed: uploaderId={}, subjectId={}, originalFileName={}, detectedType={}, fileSizeBytes={}",
                userId, subjectId, originalFileName, detectedType, fileBytes.length);
    }

    private void scanForVirus(byte[] fileBytes, String originalFileName, UUID userId, UUID subjectId) {
        log.info("Content upload antivirus scan started: uploaderId={}, subjectId={}, originalFileName={}, fileSizeBytes={}",
                userId, subjectId, originalFileName, fileBytes.length);
        try (Socket socket = new Socket("localhost", 3310);
             OutputStream out = socket.getOutputStream();
             InputStream in = socket.getInputStream()) {

            socket.setSoTimeout(30000);

            // 1. Start INSTREAM protocol
            out.write("zINSTREAM\0".getBytes());
            out.flush();

            int offset = 0;

            while (offset < fileBytes.length) {
                int chunkSize = Math.min(2048, fileBytes.length - offset);

                // IMPORTANT: big-endian 4 byte length
                byte[] size = ByteBuffer.allocate(4)
                        .putInt(chunkSize)
                        .array();

                out.write(size);
                out.write(fileBytes, offset, chunkSize);
                out.flush();   // IMPORTANT

                offset += chunkSize;
            }

            // End stream
            out.write(new byte[]{0, 0, 0, 0});
            out.flush();

            // Read response properly
            ByteArrayOutputStream responseBuffer = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int read;

            while ((read = in.read(buffer)) != -1) {
                responseBuffer.write(buffer, 0, read);
            }

            String result = responseBuffer.toString().trim();

            if (result.contains("FOUND")) {
                log.error("Content upload rejected: antivirus detected threat, uploaderId={}, subjectId={}, originalFileName={}, scanResult={}",
                        userId, subjectId, originalFileName, result);
                throw new RuntimeException("Virus detected: " + result);
            }
            log.info("Content upload antivirus scan passed: uploaderId={}, subjectId={}, originalFileName={}",
                    userId, subjectId, originalFileName);

        } catch (IOException e) {
            log.error("Content upload antivirus scan unavailable: uploaderId={}, subjectId={}, originalFileName={}, error={}",
                    userId, subjectId, originalFileName, e.getMessage(), e);
            throw new RuntimeException("Security infrastructure unavailable: " + e.getMessage());
        }
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null) return UUID.randomUUID().toString();

        // Remove path traversal characters and replace special chars
        String clean = StringUtils.cleanPath(fileName)
                .replaceAll("[^a-zA-Z0-9._-]", "_");

        // UUID prefix prevents filename collisions in storage
        return UUID.randomUUID() + "_" + clean;
    }
    @Override
    @Transactional
    public void finalizeUploadStatus(UUID contentId, UploadResultDTO result) {
        log.info("Finalize upload status request received: contentId={}, success={}",
                contentId, result.isSuccess());
        // 1. Fetch the record we created in Step 3
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new RuntimeException("Content not found for ID: " + contentId));
        log.info("Finalize upload content resolved: contentId={}, uploaderId={}, currentStatus={}",
                content.getId(), content.getUploadedBy(), content.getVerificationStatus());

        if (result.isSuccess()) {
            // 2. SUCCESS: Update URL and set status for Admin Review
            content.setFileUrl(result.getFileUrl());
            content.setVerificationStatus(VerificationStatus.PENDING);
            log.info("Content upload finalized successfully: contentId={}, uploaderId={}, newStatus={}, fileUrlPresent={}",
                    contentId, content.getUploadedBy(), content.getVerificationStatus(), result.getFileUrl() != null);

            // 3. Step 7: Trigger Notification to Admin/User
            // notificationProducer.sendUploadSuccessEvent(content);

        } else {
            // 4. FAILURE: Mark as rejected and log the error from Upload Service
            content.setVerificationStatus(VerificationStatus.REJECTED);
            log.error("Content upload finalized as failed: contentId={}, uploaderId={}, newStatus={}, error={}",
                    contentId, content.getUploadedBy(), content.getVerificationStatus(), result.getErrorMessage());

            // notificationProducer.sendUploadFailureEvent(content, result.getErrorMessage());
        }

        contentRepository.save(content);
        log.info("Finalize upload status saved: contentId={}, uploaderId={}, status={}",
                content.getId(), content.getUploadedBy(), content.getVerificationStatus());

        try {
            //UserInternalInfoDTO userInfo = authServiceClient.getUserDetails(content.getUploadedBy());

            SummarizedContentDTO summary = SummarizedContentDTO.builder()
                    .contentId(content.getId()) // [cite: 11]
                    .title(content.getTitle()) // [cite: 12]
                    .fileUrl(content.getFileUrl())
                    .universityId(content.getUniversityId())
                    .status(content.getVerificationStatus().name())
                    .recipient("prrajpoot12234@gmail.com") // The target Gmail
                    .userId(UUID.fromString("8d56e92b-32c8-4aea-8511-75e74f7c6710"))
                    .eventType(result.isSuccess() ?
                            NotificationEventType.UPLOAD_SUCCESS :
                            NotificationEventType.UPLOAD_FAILURE)
                    .notificationTypes(List.of(NotificationType.EMAIL))
                    .build();



            notificationProducer.sendContentUploadNotification(summary,result.isSuccess());
            log.info("Content upload notification emitted: contentId={}, uploaderId={}, eventType={}, success={}",
                    content.getId(), content.getUploadedBy(), summary.getEventType(), result.isSuccess());

        } catch (Exception e) {
            log.error("Content upload notification failed: contentId={}, uploaderId={}, success={}, error={}",
                    content.getId(), content.getUploadedBy(), result.isSuccess(), e.getMessage(), e);
            throw new RuntimeException(e);
        }


    }



    @Override
    public ContentStatsDTO getStatsByProgramAndSemester(UUID programId, Integer semester) {
        log.info("Content stats request received: programId={}, semester={}", programId, semester);
        long pending = contentRepository.countByProgramIdAndSemesterAndVerificationStatus(
                programId, semester, VerificationStatus.PENDING);

        long verified = contentRepository.countByProgramIdAndSemesterAndVerificationStatus(
                programId, semester, VerificationStatus.VERIFIED);
        log.info("Content stats calculated: programId={}, semester={}, pending={}, verified={}",
                programId, semester, pending, verified);

        return new ContentStatsDTO(verified, pending);
    }

    @Override
    public UniversityContentSummaryDTO countContentGroupedByType(UUID universityId) {
        log.info("University content summary request received: universityId={}", universityId);
        // 1. Get breakdown (Status-agnostic or inclusive)
        List<ContentTypeCountDTO> breakdown = contentRepository.countContentGroupedByType(universityId);

        // 2. Get overall total (Make sure this matches the logic of the breakdown)
        // Option A: Sum the counts from the list (Cleanest)
        long total = breakdown.stream().mapToLong(ContentTypeCountDTO::count).sum();

        // Option B: Query without the VERIFIED status
        // long total = contentRepository.countByUniversityId(universityId);
        log.info("University content summary calculated: universityId={}, total={}, typeBreakdownCount={}",
                universityId, total, breakdown.size());

        return new UniversityContentSummaryDTO(total, breakdown);
    }

    @Override
    public List<PendingContentDTO> findPendingContent(UUID scopeId) {
        log.info("Pending content by scope request received: scopeId={}", scopeId);
        // 1. Fetch from DB
        List<PendingContentDTO> pendingContent = contentRepository.findByVerificationStatusAndUniversityIdOrDepartmentIdOrProgramId(
                        VerificationStatus.PENDING, scopeId, scopeId, scopeId)
                .stream()
                .map(content -> new PendingContentDTO(
                        content.getId(),
                        content.getTitle(),
                        content.getDescription(),
                        "User ID: " + content.getUploadedBy(), // Placeholder for User Service Name
                        content.getFileUrl(),
                        content.getFileType().name(),
                        content.getCreatedAt(),
                        "Subject ID: " + content.getSubjectId(), // Placeholder for Subject Service Name
                        content.getContentType()

                ))
                .collect(Collectors.toList());
        log.info("Pending content by scope loaded: scopeId={}, pendingCount={}",
                scopeId, pendingContent.size());
        return pendingContent;
    }

    @Override
    public List<Content> search(ContentSearchRequest request) {
        log.info("Content search request received: universityId={}, departmentId={}, programId={}, semester={}, subjectId={}, examTypeId={}, academicYear={}, contentType={}",
                request.getUniversityId(),
                request.getDepartmentId(),
                request.getProgramId(),
                request.getSemester(),
                request.getSubjectId(),
                request.getExamTypeId(),
                request.getAcademicYear(),
                request.getContentType());
        List<Content> results = contentRepository.findAll(ContentSpecifications.withFilters(request));
        log.info("Content search completed: resultCount={}", results.size());
        return results;
    }

    @Override
    public List<PendingContentDTO> getPendingContent(UUID programId, Integer academicYear, VerificationStatus verificationStatus) {
        log.info("Pending content by session request received: programId={}, academicYear={}, verificationStatus={}",
                programId, academicYear, verificationStatus);
        List<PendingContentDTO> pendingContent = contentRepository.findByProgramIdAndAcademicYearAndVerificationStatus(
                        programId, academicYear, verificationStatus)
                .stream()
                .map(c -> new PendingContentDTO(
                        c.getId(),              // contentId
                        c.getTitle(),           // title
                        c.getDescription(),     // description
                        "ID: " + c.getUploadedBy(), // uploaderName (placeholder)
                        c.getFileUrl(),         // fileUrl
                        c.getFileType().name(), // fileType
                        c.getCreatedAt(),       // uploadedAt
                        "Subject: " + c.getSubjectId(), // subjectName (placeholder)
                        c.getContentType()      // contentType
                )).toList();
        log.info("Pending content by session loaded: programId={}, academicYear={}, verificationStatus={}, pendingCount={}",
                programId, academicYear, verificationStatus, pendingContent.size());
        return pendingContent;
    }


    @Override

    public void verifyOrRejectContent(UUID contentId, String status, UUID verifiedBy) {
        log.info("Verify or reject content request received: contentId={}, requestedStatus={}, verifiedBy={}",
                contentId, status, verifiedBy);
        // 1. Update DB Status
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new RuntimeException("Content not found"));
        log.info("Verify or reject content resolved: contentId={}, uploaderId={}, currentStatus={}",
                content.getId(), content.getUploadedBy(), content.getVerificationStatus());

        VerificationStatus newStatus = VerificationStatus.valueOf(status.toUpperCase());
        content.setVerificationStatus(newStatus);
        content.setVerifiedBy(verifiedBy); // Audit trail
        content.setVerifiedAt(LocalDateTime.now());
        contentRepository.save(content);
        log.info("Content verification status updated: contentId={}, uploaderId={}, verifiedBy={}, newStatus={}",
                content.getId(), content.getUploadedBy(), verifiedBy, newStatus);

        // 2. Trigger Notification Flow
        handleStatusNotification(content);
    }

    private void handleStatusNotification(Content content) {
        try {
            log.info("Content status notification started: contentId={}, uploaderId={}, status={}",
                    content.getId(), content.getUploadedBy(), content.getVerificationStatus());
            // A. Fetch the actual email of the uploader from User Service
            UserInternalDTO uploader = userServiceClient.getUserInternalInfo(content.getUploadedBy());

            log.info("Content uploader info loaded for notification: contentId={}, uploaderId={}, emailPresent={}",
                    content.getId(), content.getUploadedBy(), uploader != null && uploader.email() != null);

            // B. Build the Summary with real recipient data
            SummarizedContentDTO summary = SummarizedContentDTO.builder()
                    .contentId(content.getId())
                    .title(content.getTitle())
                    .userId(content.getUploadedBy())
                    .recipient(uploader.email()) // Dynamic email!
                    .status(content.getVerificationStatus().name())
                    .eventType(content.getVerificationStatus() == VerificationStatus.VERIFIED
                            ? NotificationEventType.CONTENT_VERIFIED
                            : NotificationEventType.CONTENT_REJECTED)
                    .build();

            // C. Emit to Kafka
            notificationProducer.sendContentStatusUpdateNotification(summary);
            log.info("Content status notification emitted: contentId={}, uploaderId={}, eventType={}, status={}",
                    content.getId(), content.getUploadedBy(), summary.getEventType(), content.getVerificationStatus());

        } catch (Exception e) {
            log.error("Content status updated but notification failed: contentId={}, uploaderId={}, status={}, error={}",
                    content.getId(), content.getUploadedBy(), content.getVerificationStatus(), e.getMessage(), e);
        }
    }

}

