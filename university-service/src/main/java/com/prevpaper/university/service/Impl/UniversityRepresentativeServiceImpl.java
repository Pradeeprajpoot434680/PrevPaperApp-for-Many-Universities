package com.prevpaper.university.service.Impl;

import com.prevpaper.comman.enums.ScopeType;
import com.prevpaper.comman.enums.UserRole;
import com.prevpaper.comman.exception.BusinessException;
import com.prevpaper.comman.exception.ResourceNotFoundException;
import com.prevpaper.university.dtos.AssignRepRequest;
import com.prevpaper.university.dtos.DepartmentRequest;
import com.prevpaper.university.entities.Department;
import com.prevpaper.university.entities.RepresentativeAssignment;
import com.prevpaper.university.entities.University;
import com.prevpaper.university.repository.DepartmentRepository;
import com.prevpaper.university.repository.RepresentativeRepository;
import com.prevpaper.university.repository.UniversityRepository;
import com.prevpaper.university.service.UniversityRepresentativeService;
import com.prevpaper.university.utils.EmitRoleAssignment;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class UniversityRepresentativeServiceImpl implements UniversityRepresentativeService {

    private final DepartmentRepository departmentRepository;
    private final RepresentativeRepository representativeRepository;
    private final EmitRoleAssignment emitRoleAssignment;
    private  final UniversityRepository universityRepository;

    @Override
    @Transactional
    public Department createDepartment(UUID universityId, DepartmentRequest request) {

        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Department name is required");
        }

        if (request.getCode() == null || request.getCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Department code is required");
        }

        String name = request.getName().trim();
        String code = request.getCode().trim().toUpperCase();

        // check duplicate code in same university
        if (departmentRepository.existsByCodeAndUniversityId(code, universityId)) {
            throw new RuntimeException("Department with code " + code + " already exists");
        }

        // optional: check duplicate name
        if (departmentRepository.existsByNameIgnoreCaseAndUniversityId(name, universityId)) {
            throw new RuntimeException("Department with name " + name + " already exists");
        }

        Department department = Department.builder()
                .name(name)
                .code(code)
                .university(University.builder().id(universityId).build())
                .build();

        return departmentRepository.save(department);
    }


    @Override
    public void assignDepartmentRep(AssignRepRequest request, UUID adminId) {
        // Logic: Assign a user to a specific Department scope
        try {
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

            emitRoleAssignment.sendEmittedRoleAssignmentToKafka(request.getUserId(),3,request.getScopeId());

        } catch (BusinessException e) {
            throw new BusinessException("Error in business logix");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public boolean existsById(UUID universityId) {
        return universityRepository.existsById(universityId);
    }
}
