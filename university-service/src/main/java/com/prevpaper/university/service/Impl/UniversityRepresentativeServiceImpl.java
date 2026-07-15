package com.prevpaper.university.service.Impl;

import com.prevpaper.comman.dto.ApiResponse;
import com.prevpaper.comman.enums.ScopeType;
import com.prevpaper.comman.enums.UserRole;
import com.prevpaper.comman.exception.BusinessException;
import com.prevpaper.comman.exception.ResourceAlreadyExist;
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
import org.springframework.cache.annotation.CacheEvict; // 🟢 IMPORTED
import org.springframework.cache.annotation.Cacheable; // 🟢 IMPORTED
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
    private final UniversityRepository universityRepository;
    private final ProgramRepository programRepository;
    private final ExamConfigRepository examConfigRepository;

    /**
     * MUTATION: Evicts 'departments' cache for this specific university
     * so the frontend instantly sees the new department.
     */
//    @Override
//    @Transactional
//    @CacheEvict(value = "departments", key = "#universityId") // 🟢 EVICTS OLD CACHE
//    public Department createDepartment(UUID universityId, DepartmentRequest request) {
//        log.info("Redis Cache EVICT [departments] - Creating department for universityId={}", universityId);
//
//        if (request.getName() == null || request.getName().trim().isEmpty()) {
//            throw new IllegalArgumentException("Department name is required");
//        }
//        if (request.getCode() == null || request.getCode().trim().isEmpty()) {
//            throw new IllegalArgumentException("Department code is required");
//        }
//
//        String name = request.getName().trim();
//        String code = request.getCode().trim().toUpperCase();
//
//        if (departmentRepository.existsByCodeAndUniversityId(code, universityId)) {
//            throw new RuntimeException("Department with code " + code + " already exists");
//        }
//        if (departmentRepository.existsByNameIgnoreCaseAndUniversityId(name, universityId)) {
//            throw new RuntimeException("Department with name " + name + " already exists");
//        }
//
//        Department department = Department.builder()
//                .name(name)
//                .code(code)
//                .university(University.builder().id(universityId).build())
//                .build();
//
//        return departmentRepository.save(department);
//    }


    @Override
    @Transactional
    @CacheEvict(value = "departments", key = "#universityId")
    public DepartmentTinyDTO createDepartment(UUID universityId, DepartmentRequest request) {
        log.info("Redis Cache EVICT [departments] - Creating department for universityId={}", universityId);

        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Department name is required");
        }
        if (request.getCode() == null || request.getCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Department code is required");
        }

        String name = request.getName().trim();
        String code = request.getCode().trim().toUpperCase();

        if (departmentRepository.existsByCodeAndUniversityId(code, universityId)) {
            throw new RuntimeException("Department with code " + code + " already exists");
        }
        if (departmentRepository.existsByNameIgnoreCaseAndUniversityId(name, universityId)) {
            throw new RuntimeException("Department with name " + name + " already exists");
        }

        Department department = Department.builder()
                .name(name)
                .code(code)
                .university(University.builder().id(universityId).build())
                .build();

        Department savedDepartment = departmentRepository.save(department);

        // 🟢 RETURN A CLEAN TIMESTAMPL-LESS DTO INSTEAD OF THE JPA ENTITY
        return new DepartmentTinyDTO(
                savedDepartment.getId(),
                savedDepartment.getName(),
                savedDepartment.getCode()
        );
    }



    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "departments", key = "#universityId"),
            @CacheEvict(value = "universityTeam", key = "#universityId"),
            // 🟢 CRITICAL FIX: Explicitly flushes the department-rep lists view cache instantly
            @CacheEvict(value = "deptReps", allEntries = true),
            @CacheEvict(value = "programReps", allEntries = true)
    })
    public void assignDepartmentRep(AssignRepRequest request, UUID adminId) {
        try {
            log.info("Assign department representative request received: userId={}, scopeId={}", request.getUserId(), request.getScopeId());

            boolean alreadyAssigned = representativeRepository
                    .existsByUserIdAndScopeIdAndIsActiveTrue(request.getUserId(), request.getScopeId());

            if (alreadyAssigned) {
                throw new ResourceAlreadyExist("This user is already an active representative for this specific department scope.");
            }
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
            emitRoleAssignment.sendEmittedRoleAssignmentToKafka(request.getUserId(), 3, request.getScopeId());

        } catch (Exception e) {
            log.error("Department representative assignment failed: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean existsById(UUID universityId) {
        return universityRepository.existsById(universityId);
    }

    /**
     * READ CACHE: Caches the absolute global list of all registered universities.
     */
    // 1. Keep this method clean WITHOUT @Cacheable to avoid entity proxy traps
    @Override
    public List<UniversityResponseDTO> findAll() {
        log.info("Loading global universities list from DB");
        return universityRepository.findAll()
                .stream()
                .map(u -> new UniversityResponseDTO(u.getId(), u.getName(), u.getCode(), u.getState(), u.getCity()))
                .toList();
    }

    // 2. Put the cache annotation on a clean, DTO-only layer function
    @Override
    @Cacheable(value = "allUniversitiesDTO")
    public List<UniversityResponseDTO> getCachedUniversities() {
        log.info("Redis Cache MISS - Loading university DTOs from DB data");
        return this.findAll();
    }

    /**
     * READ CACHE: Caches department lists by unique university ID.
     */
    @Override
    @Cacheable(value = "departments", key = "#universityId") // 🟢 CACHED BY ID
    public List<DepartmentDTO> findDepartmentByUniversityId(UUID universityId) {
        log.info("Redis Cache MISS - Loading departments from DB for university: {}", universityId);
        return departmentRepository.findByUniversityId(universityId)
                .stream()
                .map(dept -> new DepartmentDTO(dept.getId(), dept.getName()))
                .collect(Collectors.toList());
    }

    /**
     * READ CACHE: Caches program lists by unique department ID.
     */
    @Override
    @Cacheable(value = "programs", key = "#departmentId") // 🟢 CACHED BY ID
    public List<ProgramDTO> findProgramByDepartmentId(UUID departmentId) {
        log.info("Redis Cache MISS - Loading programs from DB for department: {}", departmentId);
        return programRepository.findByDepartmentId(departmentId)
                .stream()
                .map(prog -> new ProgramDTO(prog.getId(), prog.getName()))
                .collect(Collectors.toList());
    }

    /**
     * MUTATION: Evicts exam configurations for this university upon adding a new format.
     */
    @Override
    @Transactional
    @CacheEvict(value = "examConfigs", key = "#universityId") // 🟢 EVICTS OLD CACHE
    public ApiResponse<String> addExamFormat(UUID universityId, ExamFormatRequest format) {
        log.info("Redis Cache EVICT [examConfigs] - Adding exam configuration format for universityId={}", universityId);

        University university = universityRepository.findById(universityId)
                .orElseThrow(() -> new ResourceNotFoundException("University not found"));

        List<ExamConfiguration> existingConfigs = examConfigRepository.findByUniversity(university);

        Set<String> existingNames = existingConfigs.stream()
                .map(config -> config.getExamName().toLowerCase()).collect(Collectors.toSet());

        Set<Integer> existingOrders = existingConfigs.stream()
                .map(ExamConfiguration::getDisplayOrder).collect(Collectors.toSet());

        String normalizedName = format.examName().trim();
        Integer order = format.displayOrder();

        if (existingNames.contains(normalizedName.toLowerCase())) {
            throw new BadRequestException("Exam name '" + normalizedName + "' already exists for this university.");
        }
        if (existingOrders.contains(order)) {
            throw new BadRequestException("Display order " + order + " is already assigned to another exam.");
        }

        ExamConfiguration newConfig = ExamConfiguration.builder()
                .examName(normalizedName)
                .displayOrder(order)
                .university(university)
                .isActive(true)
                .build();

        examConfigRepository.save(newConfig);
        return ApiResponse.success("Successfully added new exam format: " + normalizedName, null);
    }

    /**
     * READ CACHE: Caches custom configured exam types by university context.
     */
    @Override
    @Cacheable(value = "examConfigs", key = "#universityId") // 🟢 CACHED BY ID
    public ApiResponse<List<ExamConfigurationDTO>> getExamTypes(UUID universityId) {
        log.info("Redis Cache MISS - Loading configured exam formats from DB for university: {}", universityId);
        University university = universityRepository.findById(universityId)
                .orElseThrow(() -> new ResourceNotFoundException("University not found"));

        List<ExamConfiguration> examTypes = examConfigRepository.findByUniversity(university);

        List<ExamConfigurationDTO> dtos = examTypes.stream()
                .map(ec -> new ExamConfigurationDTO(ec.getId(), ec.getExamName(), ec.getDisplayOrder()))
                .toList();

        return ApiResponse.success("Successfully Fetched Exam Types", dtos);
    }
}