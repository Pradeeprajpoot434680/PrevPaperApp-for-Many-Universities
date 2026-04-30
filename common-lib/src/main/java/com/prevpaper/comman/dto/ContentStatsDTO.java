package com.prevpaper.comman.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContentStatsDTO {
    private long verifiedCount;
    private long pendingCount;
}