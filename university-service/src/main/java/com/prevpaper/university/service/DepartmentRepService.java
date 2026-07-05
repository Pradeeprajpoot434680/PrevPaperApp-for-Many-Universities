package com.prevpaper.university.service;

import com.prevpaper.university.dtos.AssignRepRequest;
import com.prevpaper.university.dtos.DepartmentTinyDTO;
import com.prevpaper.university.dtos.ProgramRequest;
import com.prevpaper.university.dtos.ProgramResponseDTO;
import com.prevpaper.university.entities.AcademicSession;
import com.prevpaper.university.entities.Department;
import com.prevpaper.university.entities.Program;

import java.util.List;
import java.util.UUID;

public interface DepartmentRepService {
    ProgramResponseDTO createProgram(UUID departmentId, ProgramRequest request);
    void assignProgramRep(AssignRepRequest request, UUID adminId);
    List<DepartmentTinyDTO> findDepartmentsByUniversityId(UUID universityId);
}
