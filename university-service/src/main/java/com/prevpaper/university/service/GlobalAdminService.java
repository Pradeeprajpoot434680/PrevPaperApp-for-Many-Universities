package com.prevpaper.university.service;


import com.prevpaper.university.dtos.*;
import com.prevpaper.university.entities.University;

import java.util.List;
import java.util.UUID;

public interface GlobalAdminService {
    UniversitySaveResponseDTO createUniversity(UniversityRequest universityRequest);
    void assignUniversityRep(AssignRepRequest request, UUID adminId);
    List<UniversityDashboardDTO> getUniversityDashboard();
    GlobalStatsDTO getGlobalStats();

    List<RepresentativeDetailsDTO> getAllUniversityReps();

}
