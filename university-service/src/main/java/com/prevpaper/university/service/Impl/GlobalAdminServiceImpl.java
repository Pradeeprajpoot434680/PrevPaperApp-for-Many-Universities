package com.prevpaper.university.service.Impl;

import com.prevpaper.comman.dto.StudentDTO;
import com.prevpaper.comman.dto.UserBatchRequest;
import com.prevpaper.comman.dto.UserDetailDTO;
import com.prevpaper.comman.enums.ScopeType;
import com.prevpaper.comman.enums.UserRole;
import com.prevpaper.comman.producer.RoleEventProducer;
import com.prevpaper.university.client.AuthClient;
import com.prevpaper.university.client.UserServiceClient;
import com.prevpaper.university.dtos.*;
import com.prevpaper.university.entities.RepresentativeAssignment;
import com.prevpaper.university.entities.University;
import com.prevpaper.university.repository.RepresentativeRepository;
import com.prevpaper.university.repository.UniversityRepository;
import com.prevpaper.university.service.GlobalAdminService;
import com.prevpaper.university.utils.EmitRoleAssignment;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GlobalAdminServiceImpl implements GlobalAdminService {

    private final UniversityRepository universityRepository;
    private final RepresentativeRepository representativeRepository;
    private final RoleEventProducer roleEventProducer;
    private final EmitRoleAssignment emitRoleAssignment;
    private final UserServiceClient userServiceClient;
    private final AuthClient authClient;

    public GlobalAdminServiceImpl(UniversityRepository universityRepository,
                                  RepresentativeRepository representativeRepository,
                                  RoleEventProducer roleEventProducer,
                                  EmitRoleAssignment emitRoleAssignment,
                                  UserServiceClient userServiceClient,
                                  AuthClient authClient) {
        this.universityRepository = universityRepository;
        this.representativeRepository = representativeRepository;
        this.roleEventProducer = roleEventProducer;
        this.emitRoleAssignment = emitRoleAssignment;
        this.userServiceClient = userServiceClient;
        this.authClient = authClient;
    }

    /**
     * MUTATION: Evicts the global university arrays and statistics layouts so
     * the new platform entity registers in realtime on the dashboard lists.
     */
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "allUniversities", allEntries = true),
            @CacheEvict(value = "globalStats", allEntries = true),
            @CacheEvict(value = "globalUniversityDashboard", allEntries = true)
    })
    public UniversitySaveResponseDTO createUniversity(UniversityRequest request) {
        log.info("Redis Cache EVICT [allUniversities, globalStats, globalUniversityDashboard] - Creating university: {}", request.getName());

        if (universityRepository.existsByName(request.getName())) {
            throw new RuntimeException("University with name '" + request.getName() + "' already exists");
        }
        if (universityRepository.existsByCode(request.getCode())) {
            throw new RuntimeException("University code '" + request.getCode() + "' is already in use");
        }

        String slug = (request.getSlug() != null && !request.getSlug().isBlank())
                ? request.getSlug()
                : request.getName().toLowerCase().replaceAll("\\s+", "-");

        University university = University.builder()
                .name(request.getName())
                .code(request.getCode().toUpperCase())
                .slug(slug)
                .description(request.getDescription())
                .country(request.getCountry())
                .state(request.getState())
                .city(request.getCity())
                .websiteUrl(request.getWebsiteUrl())
                .emailDomain(request.getEmailDomain())
                .logoUrl(request.getLogoUrl())
                .active(true)
                .build();

        University savedUniversity = universityRepository.save(university);
        log.info("University saved to database with ID: {}", savedUniversity.getId());

        // 🟢 FIXED: Map to a clean, timestamp-free DTO before returning
        return new UniversitySaveResponseDTO(
                savedUniversity.getId(),
                savedUniversity.getName(),
                savedUniversity.getCode(),
                savedUniversity.getSlug(),
                savedUniversity.getDescription(),
                savedUniversity.getCountry(),
                savedUniversity.getState(),
                savedUniversity.getCity(),
                savedUniversity.getLogoUrl(),
                savedUniversity.getWebsiteUrl(),
                savedUniversity.getEmailDomain(),
                savedUniversity.getActive()
        );
    }

    /**
     * MUTATION: Evicts current dashboard tables and representatives tracking models.
     */
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "globalUniversityDashboard", allEntries = true),
            @CacheEvict(value = "allUniversityReps", allEntries = true),
            @CacheEvict(value = "globalStats", allEntries = true),
            @CacheEvict(value = "universityTeam", key = "#request.scopeId")
    })
    public void assignUniversityRep(AssignRepRequest request, UUID adminId) {
        log.info("Redis Cache EVICT - Assigning university representative for scopeId={}", request.getScopeId());

        RepresentativeAssignment assignment = RepresentativeAssignment.builder()
                .userId(request.getUserId())
                .roles(Set.of(UserRole.UNIVERSITY_ADMIN))
                .scopeType(ScopeType.UNIVERSITY)
                .scopeId(request.getScopeId())
                .isActive(true)
                .assignedBy(adminId)
                .expiresAt(request.getExpiresAt())
                .build();

        representativeRepository.save(assignment);

        int universityRoleId = 2;
        emitRoleAssignment.sendEmittedRoleAssignmentToKafka(request.getUserId(), universityRoleId, request.getScopeId());
    }

    /**
     * READ CACHE: Caches the heavy global admin dashboard list to avoid recurring loop Feign calls.
     */
    @Override
    @Cacheable(value = "globalUniversityDashboard")
    public List<UniversityDashboardDTO> getUniversityDashboard() {
        log.info("Redis Cache MISS - Loading global university dashboard listing from DB & User Feign");
        List<University> universities = universityRepository.findAll();

        return universities.stream().map(uni -> {
            Optional<RepresentativeAssignment> assignment = representativeRepository
                    .findByScopeIdAndScopeTypeAndIsActiveTrue(uni.getId(), ScopeType.UNIVERSITY);

            String repName = "Assign Representative";

            if (assignment.isPresent()) {
                try {
                    repName = userServiceClient.getStudentName(assignment.get().getUserId());
                } catch (Exception e) {
                    log.warn("Failed to dynamically fetch student name for uni profile card loop");
                    repName = "User Assigned (No Profile)";
                }
            }

            return new UniversityDashboardDTO(
                    uni.getId(),
                    uni.getName(),
                    uni.getCity() + ", " + uni.getState(),
                    repName,
                    uni.getActive() ? "Active" : "Inactive"
            );
        }).toList();
    }

    /**
     * READ CACHE: Caches global metrics counts (Total Unis, Total Active System-wide Reps).
     */
    @Override
    @Cacheable(value = "globalStats")
    public GlobalStatsDTO getGlobalStats() {
        log.info("Redis Cache MISS - Computing global platform metrics stats from DB");
        long totalUnis = universityRepository.count();
        long totalReps = representativeRepository.countByIsActiveTrue();

        return new GlobalStatsDTO(totalUnis, totalReps);
    }

    /**
     * READ CACHE: Caches the massive multi-service bulk fetched master representative directory.
     */
    @Override
    @Cacheable(value = "allUniversityReps")
    public List<RepresentativeDetailsDTO> getAllUniversityReps() {
        log.info("Redis Cache MISS - Processing master university representatives directory with bulk parallel Feign queries");

        List<RepresentativeAssignment> assignments = representativeRepository.findByScopeType(ScopeType.UNIVERSITY);

        List<UUID> userIds = assignments.stream()
                .map(RepresentativeAssignment::getUserId)
                .distinct()
                .toList();

        if (userIds.isEmpty()) return Collections.emptyList();

        Map<UUID, StudentDTO> userProfileMap = userServiceClient.getBulkUserDetails(userIds);

        UserBatchRequest batchRequest = new UserBatchRequest(userIds);
        List<UserDetailDTO> authDetailsList = authClient.getUserDetailsBatch(batchRequest);

        Map<UUID, UserDetailDTO> authMap = authDetailsList.stream()
                .collect(Collectors.toMap(UserDetailDTO::userId, d -> d));

        return assignments.stream().map(rep -> {
            // 🟢 FIXED: Fetch University entity directly via universityRepository (No sessionMap conflict)
            University uni = universityRepository.findById(rep.getScopeId()).orElse(null);
            StudentDTO profile = userProfileMap.get(rep.getUserId());
            UserDetailDTO auth = authMap.get(rep.getUserId());

            return new RepresentativeDetailsDTO(
                    rep.getId(),
                    rep.getUserId(),
                    profile != null ? profile.fullName() : "No Profile Created",
                    auth != null ? auth.email() : "Email Not Found",
                    uni != null ? uni.getId() : null,
                    uni != null ? uni.getName() : "Unknown University",
                    uni != null ? uni.getCode() : "N/A",
                    rep.getAssignedAt() != null ? rep.getAssignedAt().toString() : null,
                    rep.getIsActive()
            );
        }).toList();
    }
}