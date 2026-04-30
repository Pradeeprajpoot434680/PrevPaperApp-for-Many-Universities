package com.prevpaper.comman.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class UserProfileDTO {
    // Basic Profile
    private UUID userId;
    private String firstName;
    private String lastName;
    private String profileImageUrl;
    private String bio;

    // Academic Info
    private UUID universityId;
    private UUID departmentId;
    private UUID programId;
    private Integer batchYear;
    private Long totalPoints;

    // Preferences
    private String theme;
    private String language;
}