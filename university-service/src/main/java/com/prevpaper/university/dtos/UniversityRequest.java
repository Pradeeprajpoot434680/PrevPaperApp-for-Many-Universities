package com.prevpaper.university.dtos;

import lombok.Data;

@Data
public class UniversityRequest {
    private String name;
    private String code;        // e.g., "CURAJ"
    private String slug;        // optional, can be generated if not provided
    private String description;
    private String country;
    private String state;
    private String city;
    private String websiteUrl;  // optional
    private String emailDomain; // optional
    private String logoUrl;     // optional
}