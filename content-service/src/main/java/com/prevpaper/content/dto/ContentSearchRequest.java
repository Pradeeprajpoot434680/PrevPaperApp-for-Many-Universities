package com.prevpaper.content.dto;

import com.prevpaper.comman.enums.ContentType;
import lombok.Data;

import java.util.UUID;

@Data
public class ContentSearchRequest {
    private UUID universityId;
    private UUID departmentId;
    private UUID programId;
    private Integer semester;
    private UUID subjectId;
    private UUID examTypeId;
    private Integer academicYear;
    private ContentType contentType;
}