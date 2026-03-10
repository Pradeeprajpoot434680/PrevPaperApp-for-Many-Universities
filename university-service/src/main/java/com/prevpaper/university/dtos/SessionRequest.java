package com.prevpaper.university.dtos;

import lombok.Data;

import java.util.UUID;

@Data
public class SessionRequest {
    private UUID programId;
    private Integer startYear;
    private Integer endYear;
}