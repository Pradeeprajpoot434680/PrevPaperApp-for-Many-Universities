package com.prevpaper.upload.service.facade;

import com.prevpaper.upload.dto.FileMetadata;
import com.prevpaper.upload.observer.UploadObserver;
import java.util.ArrayList;
import java.util.List;

public abstract class UploadSubject {
    private final List<UploadObserver> observers = new ArrayList<>();

    public void addObserver(UploadObserver observer) {
        observers.add(observer);
    }

    protected void notifyObservers(FileMetadata metadata) {
        observers.forEach(observer -> observer.onUploadSuccess(metadata));
    }
}