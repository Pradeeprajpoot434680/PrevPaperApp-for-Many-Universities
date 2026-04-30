package com.prevpaper.university.dtos;

import java.util.UUID;

public record SubjectResourceDTO(
        UUID subjectId,
        String subjectName,
        String subjectCode
) {}