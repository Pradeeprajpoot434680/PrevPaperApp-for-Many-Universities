package com.prevpaper.university.service;

import com.prevpaper.university.dtos.AssignRepRequest;
import com.prevpaper.university.dtos.SessionDashboardDTO;
import com.prevpaper.university.dtos.SessionRepDetailsDTO;
import com.prevpaper.university.dtos.SessionRequest;
import com.prevpaper.university.entities.AcademicSession;
import com.prevpaper.university.entities.Program;

import java.util.List;
import java.util.UUID;

public interface ProgramRepService  {
    AcademicSession createSession(UUID programId,SessionRequest request);
    void assignSessionRep(AssignRepRequest request, UUID adminId);
    List<SessionDashboardDTO> getProgramSessionsDashboard(UUID programId);
    List<SessionRepDetailsDTO> getAllSessionRepsByProgram(UUID programId);
}
