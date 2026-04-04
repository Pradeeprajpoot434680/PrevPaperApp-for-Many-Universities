package com.prevpaper.university.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
public class ProgramDTO {
    private UUID id;
    private String name;
    public ProgramDTO(UUID id, String name) {
        this.id = id;
        this.name = name;
    }
}