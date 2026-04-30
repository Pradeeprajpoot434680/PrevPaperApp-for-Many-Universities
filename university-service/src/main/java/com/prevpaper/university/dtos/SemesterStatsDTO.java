package com.prevpaper.university.dtos;

import java.util.UUID;

public record SemesterStatsDTO(
        Integer semesterNumber,
        UUID semesterId,
        int subjectCount,
        long pendingVerifications, // Count of Content with status 'PENDING'
        long totalResources
) {
}
