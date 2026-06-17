package com.prevpaper.university.dtos;

import java.io.Serializable;
import java.util.UUID;

/**
 * 🟢 UNBREAKABLE DTO: Used across departments read-caches and mutation evictions.
 * Excludes all complex JPA entity attributes or LocalDateTime fields to ensure
 * 100% stable JSON serialization.
 */
public record DepartmentTinyDTO(
        UUID id,
        String name,
        String code
) implements Serializable {
    // Standard version tracking identifier for stable Redis serialization matching
    private static final long serialVersionUID = 1L;
}