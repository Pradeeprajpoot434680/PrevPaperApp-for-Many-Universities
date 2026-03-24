package com.prevpaper.upload.observer;

import com.prevpaper.upload.dto.FileMetadata;

public interface UploadObserver {
    void onUploadSuccess(FileMetadata metadata);
}
