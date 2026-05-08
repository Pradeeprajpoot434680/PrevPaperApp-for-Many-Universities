package com.prevpaper.content.dto;

import com.prevpaper.comman.enums.ContentType;

public record ContentTypeCountDTO(
        ContentType type,
        long count
) {}