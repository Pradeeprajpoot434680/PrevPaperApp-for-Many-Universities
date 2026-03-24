package com.prevpaper.content.enums;

public enum VerificationStatus {
    PENDING_UPLOAD, // Initial state: metadata saved, file not yet in cloud
    PENDING,        // File stored, waiting for Admin review
    VERIFIED,       // Visible to students
    REJECTED        // Failed validation or rejected by Admin
}
