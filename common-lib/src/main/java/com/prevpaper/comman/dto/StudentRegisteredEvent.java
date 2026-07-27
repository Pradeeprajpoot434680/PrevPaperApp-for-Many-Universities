package com.prevpaper.comman.dto;

import java.io.Serializable;
import java.util.UUID;

public record StudentRegisteredEvent(
        UUID userId,
        UUID universityId,
        UUID departmentId,
        UUID programId
) implements Serializable {}