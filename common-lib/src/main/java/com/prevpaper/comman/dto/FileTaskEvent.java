package com.prevpaper.comman.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileTaskEvent {
    private UUID contentId;
    private byte[] fileBytes;
    private String fileName;
}
