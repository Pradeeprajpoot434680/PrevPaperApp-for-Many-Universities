package com.prevpaper.university.service;

import com.prevpaper.comman.dto.ApiResponse;
import com.prevpaper.university.dtos.AcademicNamesRequest;
import com.prevpaper.university.dtos.AcademicNamesResponse;

public interface GetResourcesService {
    ApiResponse<AcademicNamesResponse> getUniversityData(AcademicNamesRequest request);
}
