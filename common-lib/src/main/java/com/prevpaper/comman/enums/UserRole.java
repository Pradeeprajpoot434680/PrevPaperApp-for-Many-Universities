package com.prevpaper.comman.enums;

import lombok.Getter;

@Getter
public enum UserRole {
    GLOBAL_ADMIN(0),
    UNIVERSITY_ADMIN(1),
    DEPT_REP(2),
    PROGRAM_REP(3),
    SESSION_REP(4),
    STUDENT(5);

    private final int hierarchyLevel;

    UserRole(int hierarchyLevel) {
        this.hierarchyLevel = hierarchyLevel;
    }
}