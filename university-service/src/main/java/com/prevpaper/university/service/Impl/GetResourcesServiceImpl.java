package com.prevpaper.university.service.Impl;

import com.prevpaper.comman.dto.ApiResponse;
import com.prevpaper.university.dtos.AcademicNamesRequest;
import com.prevpaper.university.dtos.AcademicNamesResponse;
import com.prevpaper.university.repository.DepartmentRepository;
import com.prevpaper.university.repository.ProgramRepository;
import com.prevpaper.university.repository.UniversityRepository;
import com.prevpaper.university.service.GetResourcesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor

public class GetResourcesServiceImpl  implements GetResourcesService {

    private final UniversityRepository universityRepository;
    private final ProgramRepository programRepository;
    private final DepartmentRepository departmentRepository;

    @Override
    public ApiResponse<AcademicNamesResponse> getUniversityData(AcademicNamesRequest request) {
        String uniName = universityRepository.findById(request.universityId())
                .map(u -> u.getName()).orElse("Unknown University");

        String deptName = departmentRepository.findById(request.departmentId())
                .map(d -> d.getName()).orElse("Unknown Department");

        String progName = programRepository.findById(request.programId())
                .map(p -> p.getName()).orElse("Unknown Program");

        return ApiResponse.success("Names fetched",
                new AcademicNamesResponse(uniName, deptName, progName));
    }
}
