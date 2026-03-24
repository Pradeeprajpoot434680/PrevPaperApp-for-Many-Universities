package com.prevpaper.university.service;


import com.prevpaper.university.dtos.AssignRepRequest;
import com.prevpaper.university.dtos.DepartmentRequest;
import com.prevpaper.university.entities.Department;
import jakarta.transaction.Transactional;

import java.util.UUID;

public interface UniversityRepresentativeService {
    Department createDepartment(UUID universityId,DepartmentRequest request);

//    void assignUniversityRep(AssignRepRequest request, UUID adminId);


    void assignDepartmentRep(AssignRepRequest request, UUID adminId);

    boolean existsById(UUID universityId);


}
