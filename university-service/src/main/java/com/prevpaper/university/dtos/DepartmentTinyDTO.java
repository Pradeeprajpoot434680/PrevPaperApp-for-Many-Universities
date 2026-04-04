package com.prevpaper.university.dtos;

import java.util.UUID;

public record DepartmentTinyDTO(
        UUID id,
        String name,
        String code
) {}