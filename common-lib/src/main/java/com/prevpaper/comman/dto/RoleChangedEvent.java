package com.prevpaper.comman.dto;

import java.util.UUID;


public record RoleChangedEvent(
        UUID userId,
        Integer roleId,
        UUID scopeId,
        String action // e.g., "ASSIGN" or "REMOVE"
) {
}
