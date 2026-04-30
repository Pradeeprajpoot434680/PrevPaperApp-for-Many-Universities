package com.prevpaper.university.service;

import com.prevpaper.comman.dto.StudentDTO;
import com.prevpaper.university.dtos.*;

import java.util.List;
import java.util.UUID;


public interface RepresentativeService {
    List<DepartmentRepResponse> getDeptRepsByUniversity(UUID universityId);
    List<StudentDTO> getStudentsByDepartment(UUID deptId);
    List<ProgramDashboardDTO> getDepartmentProgramsDashboard(UUID departmentId);
    List<StudentDTO> getStudentsByProgram(UUID programId);

    List<RepresentativeDetailsDTO> getProgramRepsByDept(UUID departmentId);
//    List<SessionDashboardDTO> getProgramSessionsDashboard(UUID programId);
}
