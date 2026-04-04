package com.prevpaper.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserRequest {
    private String firstName;
    private String lastName;
    private String universityId;
    private  String programId;
    private String departmentId;
    private  Integer batchYear;
}
