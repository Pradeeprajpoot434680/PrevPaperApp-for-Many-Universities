package com.prevpaper.university.service;

import com.prevpaper.comman.dto.StudentDTO;
import com.prevpaper.university.dtos.DepartmentRepResponse;

import java.util.List;
import java.util.UUID;


public interface RepresentativeService {
    List<DepartmentRepResponse> getDeptRepsByUniversity(UUID universityId);
    List<StudentDTO> getStudentsByDepartment(UUID deptId);
}
