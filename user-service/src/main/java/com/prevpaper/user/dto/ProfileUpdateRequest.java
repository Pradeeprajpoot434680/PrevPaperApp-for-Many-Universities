package com.prevpaper.user.dto;


import lombok.Data;

@Data
public class ProfileUpdateRequest {
    private String firstName;
    private String lastName;
    private String profileImageUrl;
    private String bio;
}