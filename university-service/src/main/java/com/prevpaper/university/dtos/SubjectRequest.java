package com.prevpaper.university.dtos;

import lombok.Data;

import java.util.UUID;

@Data
public class SubjectRequest {
    private String name;
    private UUID semesterId;
}
