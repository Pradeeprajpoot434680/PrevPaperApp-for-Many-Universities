package com.prevpaper.university.service;


import com.prevpaper.university.dtos.AssignRepRequest;
import com.prevpaper.university.dtos.DepartmentRequest;
import com.prevpaper.university.entities.Department;
import jakarta.transaction.Transactional;

import java.util.UUID;

public interface UniversityRepresentativeService {
    Department createDepartment(DepartmentRequest request);

//    void assignUniversityRep(AssignRepRequest request, UUID adminId);

    @Transactional
    void assignDepartmentRep(AssignRepRequest request, UUID adminId);
}
