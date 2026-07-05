package com.prevpaper.university.dtos;

import java.io.Serializable;
import java.util.UUID;

public record SessionTinyDTO(
        UUID id,
        Integer startYear,
        Integer endYear
) implements Serializable {
    private static final long serialVersionUID = 1L;
}