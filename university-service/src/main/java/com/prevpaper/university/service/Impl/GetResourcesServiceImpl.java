package com.prevpaper.university.service.Impl;

import com.prevpaper.comman.dto.ApiResponse;
import com.prevpaper.university.dtos.AcademicNamesRequest;
import com.prevpaper.university.dtos.AcademicNamesResponse;
import com.prevpaper.university.entities.University;
import com.prevpaper.university.repository.DepartmentRepository;
import com.prevpaper.university.repository.ProgramRepository;
import com.prevpaper.university.repository.UniversityRepository;
import com.prevpaper.university.service.GetResourcesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable; // 🟢 IMPORTED
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GetResourcesServiceImpl implements GetResourcesService {

    private final UniversityRepository universityRepository;
    private final ProgramRepository programRepository;
    private final DepartmentRepository departmentRepository;

    /**
     * READ CACHE: Translates compound object paths to readable string identities.
     * Generates a unique key mapping using a combined hash of the requested structural tokens.
     */
    @Override
    @Cacheable(
            value = "academicNamesLookups",
            key = "{#request.universityId(), #request.departmentId(), #request.programId()}"
    ) // 🟢 GENERATES COMPOUND COMPOSITE LOOKUP MAPPING KEYS AUTOMATICALLY
    public ApiResponse<AcademicNamesResponse> getUniversityData(AcademicNamesRequest request) {
        log.info("Redis Cache MISS - Mapping textual academic structural identity from DB for composite key paths: U={}, D={}, P={}",
                request.universityId(), request.departmentId(), request.programId());

        String uniName = universityRepository.findById(request.universityId())
                .map(University::getName).orElse("Unknown University");

        String deptName = departmentRepository.findById(request.departmentId())
                .map(com.prevpaper.university.entities.Department::getName).orElse("Unknown Department");

        String progName = programRepository.findById(request.programId())
                .map(com.prevpaper.university.entities.Program::getName).orElse("Unknown Program");

        return ApiResponse.success("Names fetched", new AcademicNamesResponse(uniName, deptName, progName));
    }
}