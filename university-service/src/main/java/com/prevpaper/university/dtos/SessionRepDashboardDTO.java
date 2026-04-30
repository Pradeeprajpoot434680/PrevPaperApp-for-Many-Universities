package com.prevpaper.university.dtos;

import java.util.List;
import java.util.UUID;

public record SessionRepDashboardDTO(
        UUID sessionId,
        String batchName,       // "Batch 2023"
        String programName,     // "B.Tech CSE"
        List<SemesterStatsDTO> semesterStats
) {
}
