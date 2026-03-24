package com.prevpaper.university.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.UUID;

@Data
public class DepartmentRequest {
    @NotBlank
    private String name;
    @NotBlank
    private String code;
}
