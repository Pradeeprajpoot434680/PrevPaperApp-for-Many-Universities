package com.prevpaper.university.service;


import com.prevpaper.comman.dto.ApiResponse;
import com.prevpaper.university.dtos.*;
import com.prevpaper.university.entities.Department;
import com.prevpaper.university.entities.ExamConfiguration;
import com.prevpaper.university.entities.University;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;

public interface UniversityRepresentativeService {
    Department createDepartment(UUID universityId,DepartmentRequest request);

//    void assignUniversityRep(AssignRepRequest request, UUID adminId);


    void assignDepartmentRep(AssignRepRequest request, UUID adminId);

    boolean existsById(UUID universityId);

    List<UniversityResponseDTO> findAll();

    List<DepartmentDTO> findDepartmentByUniversityId(UUID universityId);

    List<ProgramDTO> findProgramByDepartmentId(UUID departmentId);

    ApiResponse<String> addExamFormat(UUID universityId, ExamFormatRequest formats);

    ApiResponse<List<ExamConfigurationDTO>>getExamTypes(UUID universityId);
}
