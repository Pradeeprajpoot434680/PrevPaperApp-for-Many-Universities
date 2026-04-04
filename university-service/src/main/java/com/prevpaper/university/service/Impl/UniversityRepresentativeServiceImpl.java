package com.prevpaper.university.service.Impl;

import com.prevpaper.comman.dto.ApiResponse;
import com.prevpaper.comman.enums.ScopeType;
import com.prevpaper.comman.enums.UserRole;
import com.prevpaper.comman.exception.BusinessException;
import com.prevpaper.comman.exception.ResourceNotFoundException;
import com.prevpaper.university.dtos.*;
import com.prevpaper.university.entities.Department;
import com.prevpaper.university.entities.ExamConfiguration;
import com.prevpaper.university.entities.RepresentativeAssignment;
import com.prevpaper.university.entities.University;
import com.prevpaper.university.repository.*;
import com.prevpaper.university.service.UniversityRepresentativeService;
import com.prevpaper.university.utils.EmitRoleAssignment;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UniversityRepresentativeServiceImpl implements UniversityRepresentativeService {

    private final DepartmentRepository departmentRepository;
    private final RepresentativeRepository representativeRepository;
    private final EmitRoleAssignment emitRoleAssignment;
    private  final UniversityRepository universityRepository;
    private final ProgramRepository programRepository;
    private final ExamConfigRepository examConfigRepository;
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

    @Override
    public List<UniversityResponseDTO> findAll() {
        return universityRepository.findAll()
                .stream()
                .map(u -> new UniversityResponseDTO(
                        u.getId(),
                        u.getName(),
                        u.getCode(),
                        u.getState(),
                        u.getCity()
                ))
                .toList();
    }

    @Override
    public List<DepartmentDTO> findDepartmentByUniversityId(UUID universityId) {
        return departmentRepository.findByUniversityId(universityId)
                .stream()
                .map(dept -> new DepartmentDTO(dept.getId(), dept.getName()))
                .collect(Collectors.toList());
    }

    @Override
    public List<ProgramDTO> findProgramByDepartmentId(UUID departmentId) {
        return programRepository.findByDepartmentId(departmentId)
                .stream()
                .map(prog -> new ProgramDTO(prog.getId(), prog.getName()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ApiResponse<String> addExamFormat(UUID universityId, ExamFormatRequest format) {
        // 1. Verify University exists
        University university = universityRepository.findById(universityId)
                .orElseThrow(() -> new ResourceNotFoundException("University not found"));

        // 2. Fetch existing formats to check for duplicates
        List<ExamConfiguration> existingConfigs = examConfigRepository.findByUniversity(university);

        Set<String> existingNames = existingConfigs.stream()
                .map(config -> config.getExamName().toLowerCase())
                .collect(Collectors.toSet());

        Set<Integer> existingOrders = existingConfigs.stream()
                .map(ExamConfiguration::getDisplayOrder)
                .collect(Collectors.toSet());

        // 3. Process the single format
        String normalizedName = format.examName().trim();
        Integer order = format.displayOrder();

        // Check A: Name already exists in DB
        if (existingNames.contains(normalizedName.toLowerCase())) {
            throw new BadRequestException("Exam name '" + normalizedName + "' already exists for this university.");
        }

        // Check B: Display order already taken
        if (existingOrders.contains(order)) {
            throw new BadRequestException("Display order " + order + " is already assigned to another exam.");
        }

        // 4. Build and save new exam configuration
        ExamConfiguration newConfig = ExamConfiguration.builder()
                .examName(normalizedName)
                .displayOrder(order)
                .university(university)
                .isActive(true)
                .build();

        examConfigRepository.save(newConfig);

        return ApiResponse.success("Successfully added new exam format: " + normalizedName, null);
    }


    @Override
    public ApiResponse<List<ExamConfigurationDTO>> getExamTypes(UUID universityId) {
        University university = universityRepository.findById(universityId)
                .orElseThrow(() -> new ResourceNotFoundException("University not found"));

        List<ExamConfiguration> examTypes = examConfigRepository.findByUniversity(university);

        // Map entities to DTOs
        List<ExamConfigurationDTO> dtos = examTypes.stream()
                .map(ec -> new ExamConfigurationDTO(ec.getId(), ec.getExamName(), ec.getDisplayOrder()))
                .toList();

        return ApiResponse.success("Successfully Fetched Exam Types", dtos);
    }
}
