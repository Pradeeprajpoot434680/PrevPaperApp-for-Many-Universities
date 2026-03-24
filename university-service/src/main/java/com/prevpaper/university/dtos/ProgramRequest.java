package com.prevpaper.university.dtos;

import lombok.Data;

import java.util.UUID;

@Data
public class ProgramRequest {
    private String name;
    private String code;
}