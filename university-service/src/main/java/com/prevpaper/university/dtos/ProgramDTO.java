package com.prevpaper.university.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor; // 🟢 IMPORTED
import java.io.Serializable;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor // 🟢 FIXED: Allows Jackson to instantiate objects on Cache Hits!
@Builder
public class ProgramDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private UUID id;
    private String name;
}