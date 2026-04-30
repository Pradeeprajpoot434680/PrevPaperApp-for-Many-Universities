package com.prevpaper.university.dtos;

import java.util.List;

public record ProgramStructureDTO(
        List<IdNameDTO> semesters,
        List<IdNameDTO> sessions
) {}