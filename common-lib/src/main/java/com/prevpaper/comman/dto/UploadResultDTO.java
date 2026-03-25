package com.prevpaper.comman.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor


public class UploadResultDTO {
    private boolean success;
    private String fileUrl;
    private String errorMessage;
}
