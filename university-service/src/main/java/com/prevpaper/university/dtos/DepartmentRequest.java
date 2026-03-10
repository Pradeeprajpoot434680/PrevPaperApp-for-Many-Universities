package com.prevpaper.university.dtos;

import lombok.Data;

import java.util.UUID;

@Data
public class DepartmentRequest {
    private String name;
    private UUID universityId;
}
