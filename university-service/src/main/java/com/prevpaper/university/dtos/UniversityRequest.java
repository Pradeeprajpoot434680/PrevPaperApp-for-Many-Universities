package com.prevpaper.university.dtos;

import lombok.Data;

@Data
public class UniversityRequest {
    private String name;
    private String code; // e.g., "CURAJ"
    private String description;
    private String state;
    private String city;
}
