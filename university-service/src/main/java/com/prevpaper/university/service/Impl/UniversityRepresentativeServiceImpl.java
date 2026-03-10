package com.prevpaper.university.service.Impl;

import com.prevpaper.comman.enums.ScopeType;
import com.prevpaper.comman.enums.UserRole;
import com.prevpaper.university.dtos.AssignRepRequest;
import com.prevpaper.university.dtos.DepartmentRequest;
import com.prevpaper.university.entities.Department;
import com.prevpaper.university.entities.RepresentativeAssignment;
import com.prevpaper.university.entities.University;
import com.prevpaper.university.repository.DepartmentRepository;
import com.prevpaper.university.repository.RepresentativeRepository;
import com.prevpaper.university.service.UniversityRepresentativeService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UniversityRepresentativeServiceImpl implements UniversityRepresentativeService {

    private final DepartmentRepository departmentRepository;
    private final RepresentativeRepository representativeRepository;

    @Override
    @Transactional
    public Department createDepartment(DepartmentRequest request) {
        Department department = Department.builder()
                .name(request.getName())
                .university(University.builder().id(request.getUniversityId()).build()) //
                .build();
        // The department must be linked to a University ID
        return departmentRepository.save(department);
    }

    @Transactional
    @Override
    public void assignDepartmentRep(AssignRepRequest request, UUID adminId) {
        // Logic: Assign a user to a specific Department scope
        RepresentativeAssignment assignment = RepresentativeAssignment.builder()
                .userId(request.getUserId())
                .roles(Set.of(UserRole.DEPT_REP))
                .scopeType(ScopeType.DEPARTMENT)
                .scopeId(request.getScopeId())
                .isActive(true)
                .assignedBy(adminId)
                .assignedAt(LocalDateTime.now())
                .expiresAt(request.getExpiresAt())
                .build();

        representativeRepository.save(assignment);
    }
}
