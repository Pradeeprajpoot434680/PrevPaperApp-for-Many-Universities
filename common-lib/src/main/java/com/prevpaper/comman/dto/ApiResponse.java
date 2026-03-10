package com.prevpaper.comman.dto;
import lombok.*;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ApiResponse<T> {
    private boolean success;   // true if operation worked, false if error [cite: 5]
    private String message; // Human-readable message (e.g., "Signup successful") [cite: 5]
    private T data;           // The actual result (User ID, Token, or List) [cite: 5]
    private long timestamp;    // When the response was generated [cite: 5]



    // Helper method for quick success responses
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    // Helper method for quick error responses
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .data(null)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}