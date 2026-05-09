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
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
        log.info("Create department request received: universityId={}, name={}, code={}",
                universityId, request.getName(), request.getCode());

        if (request.getName() == null || request.getName().trim().isEmpty()) {
            log.warn("Create department rejected: missing name, universityId={}, code={}",
                    universityId, request.getCode());
            throw new IllegalArgumentException("Department name is required");
        }

        if (request.getCode() == null || request.getCode().trim().isEmpty()) {
            log.warn("Create department rejected: missing code, universityId={}, name={}",
                    universityId, request.getName());
            throw new IllegalArgumentException("Department code is required");
        }

        String name = request.getName().trim();
        String code = request.getCode().trim().toUpperCase();

        // check duplicate code in same university
        if (departmentRepository.existsByCodeAndUniversityId(code, universityId)) {
            log.warn("Create department rejected: duplicate code, universityId={}, code={}, name={}",
                    universityId, code, name);
            throw new RuntimeException("Department with code " + code + " already exists");
        }

        // optional: check duplicate name
        if (departmentRepository.existsByNameIgnoreCaseAndUniversityId(name, universityId)) {
            log.warn("Create department rejected: duplicate name, universityId={}, code={}, name={}",
                    universityId, code, name);
            throw new RuntimeException("Department with name " + name + " already exists");
        }

        Department department = Department.builder()
                .name(name)
                .code(code)
                .university(University.builder().id(universityId).build())
                .build();

        Department savedDepartment = departmentRepository.save(department);
        log.info("Department created: departmentId={}, universityId={}, name={}, code={}",
                savedDepartment.getId(), universityId, savedDepartment.getName(), savedDepartment.getCode());
        return savedDepartment;
    }


    @Override
    public void assignDepartmentRep(AssignRepRequest request, UUID adminId) {
        // Logic: Assign a user to a specific Department scope
        try {
            log.info("Assign department representative request received: userId={}, scopeId={}, adminId={}, expiresAt={}",
                    request.getUserId(), request.getScopeId(), adminId, request.getExpiresAt());
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
            log.info("Department representative assignment saved: assignmentId={}, userId={}, scopeId={}, adminId={}",
                    assignment.getId(), request.getUserId(), request.getScopeId(), adminId);

            emitRoleAssignment.sendEmittedRoleAssignmentToKafka(request.getUserId(),3,request.getScopeId());
            log.info("Department representative role emit registered: userId={}, roleId={}, scopeId={}",
                    request.getUserId(), 3, request.getScopeId());

        } catch (BusinessException e) {
            log.error("Department representative assignment failed with business error: userId={}, scopeId={}, adminId={}, error={}",
                    request.getUserId(), request.getScopeId(), adminId, e.getMessage(), e);
            throw new BusinessException("Error in business logix");
        } catch (Exception e) {
            log.error("Department representative assignment failed: userId={}, scopeId={}, adminId={}, error={}",
                    request.getUserId(), request.getScopeId(), adminId, e.getMessage(), e);
            throw new RuntimeException(e);
        }

    }

    @Override
    public boolean existsById(UUID universityId) {
        boolean exists = universityRepository.existsById(universityId);
        log.info("University existence checked: universityId={}, exists={}", universityId, exists);
        return exists;
    }

    @Override
    public List<UniversityResponseDTO> findAll() {
        log.info("Find all universities request received");
        List<UniversityResponseDTO> universities = universityRepository.findAll()
                .stream()
                .map(u -> new UniversityResponseDTO(
                        u.getId(),
                        u.getName(),
                        u.getCode(),
                        u.getState(),
                        u.getCity()
                ))
                .toList();
        log.info("Find all universities completed: universityCount={}", universities.size());
        return universities;
    }

    @Override
    public List<DepartmentDTO> findDepartmentByUniversityId(UUID universityId) {
        log.info("Find departments by university request received: universityId={}", universityId);
        List<DepartmentDTO> departments = departmentRepository.findByUniversityId(universityId)
                .stream()
                .map(dept -> new DepartmentDTO(dept.getId(), dept.getName()))
                .collect(Collectors.toList());
        log.info("Find departments by university completed: universityId={}, departmentCount={}",
                universityId, departments.size());
        return departments;
    }

    @Override
    public List<ProgramDTO> findProgramByDepartmentId(UUID departmentId) {
        log.info("Find programs by department request received: departmentId={}", departmentId);
        List<ProgramDTO> programs = programRepository.findByDepartmentId(departmentId)
                .stream()
                .map(prog -> new ProgramDTO(prog.getId(), prog.getName()))
                .collect(Collectors.toList());
        log.info("Find programs by department completed: departmentId={}, programCount={}",
                departmentId, programs.size());
        return programs;
    }

    @Override
    @Transactional
    public ApiResponse<String> addExamFormat(UUID universityId, ExamFormatRequest format) {
        log.info("Add exam format request received: universityId={}, examName={}, displayOrder={}",
                universityId, format.examName(), format.displayOrder());
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
            log.warn("Add exam format rejected: duplicate exam name, universityId={}, examName={}",
                    universityId, normalizedName);
            throw new BadRequestException("Exam name '" + normalizedName + "' already exists for this university.");
        }

        // Check B: Display order already taken
        if (existingOrders.contains(order)) {
            log.warn("Add exam format rejected: duplicate display order, universityId={}, examName={}, displayOrder={}",
                    universityId, normalizedName, order);
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
        log.info("Exam format added: examConfigId={}, universityId={}, examName={}, displayOrder={}",
                newConfig.getId(), universityId, normalizedName, order);

        return ApiResponse.success("Successfully added new exam format: " + normalizedName, null);
    }


    @Override
    public ApiResponse<List<ExamConfigurationDTO>> getExamTypes(UUID universityId) {
        log.info("Get exam types request received: universityId={}", universityId);
        University university = universityRepository.findById(universityId)
                .orElseThrow(() -> new ResourceNotFoundException("University not found"));

        List<ExamConfiguration> examTypes = examConfigRepository.findByUniversity(university);

        // Map entities to DTOs
        List<ExamConfigurationDTO> dtos = examTypes.stream()
                .map(ec -> new ExamConfigurationDTO(ec.getId(), ec.getExamName(), ec.getDisplayOrder()))
                .toList();
        log.info("Get exam types completed: universityId={}, examTypeCount={}", universityId, dtos.size());

        return ApiResponse.success("Successfully Fetched Exam Types", dtos);
    }
}
