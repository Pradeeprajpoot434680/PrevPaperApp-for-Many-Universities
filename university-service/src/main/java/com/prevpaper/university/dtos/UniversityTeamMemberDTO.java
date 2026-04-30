package com.prevpaper.university.dtos;

import com.prevpaper.comman.enums.ScopeType;

import java.util.UUID;

public record UniversityTeamMemberDTO(
        UUID userId,
        String firstName,
        String lastName,
        String profileImageUrl,
        String roleName,    // e.g., "DEPARTMENT_REP"
        String scopeName,   // e.g., "Computer Science Dept"
        ScopeType scopeType
) {
}
