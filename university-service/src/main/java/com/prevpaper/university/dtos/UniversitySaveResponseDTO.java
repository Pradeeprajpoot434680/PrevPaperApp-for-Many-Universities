package com.prevpaper.university.dtos;

import java.io.Serializable;
import java.util.UUID;

/**
 * 🟢 MUTATION RESPONSE DTO: Used exclusively to send clean, serializable responses
 * back to the browser upon creating a university, keeping it 100% immune from LocalDateTime bugs.
 */
public record UniversitySaveResponseDTO(
        UUID id,
        String name,
        String code,
        String slug,
        String description,
        String country,
        String state,
        String city,
        String logoUrl,
        String websiteUrl,
        String emailDomain,
        Boolean active
) implements Serializable {
    private static final long serialVersionUID = 1L;
}