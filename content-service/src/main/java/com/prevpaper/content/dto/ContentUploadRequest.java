package com.prevpaper.content.dto;


import com.prevpaper.content.enums.ContentType;
import com.prevpaper.content.enums.FileType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContentUploadRequest {

    @NotBlank(message = "Title is required")
    private String title; // [cite: 12]

    private String description; // [cite: 13]

    @NotNull(message = "Content type (PAPER/NOTES/SOLUTION) is required")
    private ContentType contentType; // [cite: 14]

    // Educational Hierarchy
    @NotNull(message = "University ID is required")
    private UUID universityId;

    @NotNull(message = "Department ID is required")
    private UUID departmentId;

    @NotNull(message = "Program ID is required")
    private UUID programId;

    @NotNull(message = "Academic Year is required")
    private Integer academicYear;

    @NotNull(message = "Semester is required")
    private Integer semester;

    @NotNull(message = "Subject ID is required")
    private UUID subjectId;

    @NotNull(message = "File type (PDF/IMAGE) is required")
    private FileType fileType; // [cite: 24]

    private UUID teacherId; // Optional [cite: 21]
}
