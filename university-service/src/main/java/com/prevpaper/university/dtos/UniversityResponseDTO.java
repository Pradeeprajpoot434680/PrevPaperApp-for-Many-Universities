package com.prevpaper.university.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UniversityResponseDTO {
    private UUID id;
    private String name;

    private String code;
    private String state;
    private String city;
}
