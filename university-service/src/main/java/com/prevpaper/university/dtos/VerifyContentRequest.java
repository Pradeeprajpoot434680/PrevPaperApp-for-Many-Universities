package com.prevpaper.university.dtos;

public record VerifyContentRequest(
        String status // "VERIFIED" or "REJECTED"
) {
}
