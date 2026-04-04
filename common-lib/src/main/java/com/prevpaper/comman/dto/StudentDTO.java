package com.prevpaper.comman.dto;

import java.util.UUID;

public record StudentDTO(
        UUID authUserId,    // Used to create the RepresentativeAssignment
        String fullName,    // Displayed in the dropdown
        String email
) {
}
