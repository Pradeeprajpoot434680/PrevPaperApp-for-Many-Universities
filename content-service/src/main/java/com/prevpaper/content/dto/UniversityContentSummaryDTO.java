package com.prevpaper.content.dto;

import java.util.List;

public record UniversityContentSummaryDTO(
        long totalContent,
        List<ContentTypeCountDTO> breakdown
) {}