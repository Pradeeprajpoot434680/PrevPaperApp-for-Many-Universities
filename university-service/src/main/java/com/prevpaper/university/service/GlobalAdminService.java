package com.prevpaper.university.service;


import com.prevpaper.university.dtos.AssignRepRequest;
import com.prevpaper.university.dtos.UniversityRequest;
import com.prevpaper.university.entities.University;

import java.util.UUID;

public interface GlobalAdminService {
    University createUniversity(UniversityRequest universityRequest);
    void assignUniversityRep(AssignRepRequest request, UUID adminId);

}
