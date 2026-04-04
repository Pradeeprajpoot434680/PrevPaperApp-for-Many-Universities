package com.prevpaper.university.dtos;

import java.util.UUID;

public record ExamConfigurationDTO (
        UUID id,
        String examName,
        Integer displayOrder
) {
}
