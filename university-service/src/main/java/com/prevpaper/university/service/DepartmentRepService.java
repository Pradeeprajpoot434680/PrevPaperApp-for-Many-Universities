package com.prevpaper.university.service;

import com.prevpaper.university.dtos.AssignRepRequest;
import com.prevpaper.university.dtos.ProgramRequest;
import com.prevpaper.university.entities.AcademicSession;
import com.prevpaper.university.entities.Program;

import java.util.UUID;

public interface DepartmentRepService {
    Program createProgram(UUID departmentId,ProgramRequest request);
    void assignProgramRep(AssignRepRequest request, UUID adminId);

}
