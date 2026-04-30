package com.prevpaper.university.dtos;

import java.util.UUID;

public record AcademicNamesRequest(
        UUID universityId,
        UUID departmentId,
        UUID programId
) {}
