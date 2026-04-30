package com.prevpaper.content.dto;

import com.prevpaper.content.enums.ContentType;

public record ContentTypeCountDTO(
        ContentType type,
        long count
) {}