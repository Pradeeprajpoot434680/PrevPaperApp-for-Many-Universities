package com.prevpaper.content.service;

import com.prevpaper.comman.dto.UploadResultDTO;
import com.prevpaper.content.dto.ContentUploadRequest;
import com.prevpaper.content.entities.Content;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface ContentService {

    // Step 2, 3, 4: Save metadata (URL=NULL) and emit Kafka event
    Content initiateUpload(ContentUploadRequest request, MultipartFile file, UUID userId);

    // Step 6: Callback from Upload Service to set URL and update Status
    void finalizeUploadStatus(UUID contentId, UploadResultDTO result);

}
