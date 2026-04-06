package com.prevpaper.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StudentsOfUniversityDTO {
    private  UUID authUserId;
    private String email;
    private String fullName;
}
