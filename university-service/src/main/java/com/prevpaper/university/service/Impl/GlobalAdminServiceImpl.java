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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GlobalAdminServiceImpl implements GlobalAdminService {

    private final UniversityRepository universityRepository;
    private final RepresentativeRepository representativeRepository;
    private  final RoleEventProducer roleEventProducer;
    private final EmitRoleAssignment emitRoleAssignment;
    private final UserServiceClient userServiceClient;
    private final AuthClient authClient;

    public GlobalAdminServiceImpl(UniversityRepository universityRepository, RepresentativeRepository representativeRepository, RoleEventProducer roleEventProducer, EmitRoleAssignment emitRoleAssignment, UserServiceClient userServiceClient, AuthClient authClient) {
        this.universityRepository = universityRepository;
        this.representativeRepository = representativeRepository;
        this.roleEventProducer = roleEventProducer;
        this.emitRoleAssignment = emitRoleAssignment;
        this.userServiceClient = userServiceClient;
        this.authClient = authClient;
    }

    @Override
    @Transactional
    public University createUniversity(UniversityRequest request) {
        log.info("Create university request received: name={}, code={}, city={}, state={}, country={}",
                request.getName(), request.getCode(), request.getCity(), request.getState(), request.getCountry());
        // 1. Validation checks
        if (universityRepository.existsByName(request.getName())) {
            log.warn("Create university rejected: duplicate name, name={}, code={}",
                    request.getName(), request.getCode());
            throw new RuntimeException("University with name '" + request.getName() + "' already exists");
        }

        if (universityRepository.existsByCode(request.getCode())) {
            log.warn("Create university rejected: duplicate code, name={}, code={}",
                    request.getName(), request.getCode());
            throw new RuntimeException("University code '" + request.getCode() + "' is already in use");
        }

        // 2. Build slug if not provided
        String slug = (request.getSlug() != null && !request.getSlug().isBlank())
                ? request.getSlug()
                : request.getName().toLowerCase().replaceAll("\\s+", "-");

        // 3. Build the University entity
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
        log.info("University created: universityId={}, name={}, code={}, active={}",
                savedUniversity.getId(), savedUniversity.getName(), savedUniversity.getCode(), savedUniversity.getActive());
        return savedUniversity;
    }
    private String generateSlug(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "") // remove special chars
                .replaceAll("\\s+", "-");        // replace spaces with hyphens
    }

    @Override

    public void assignUniversityRep(AssignRepRequest request, UUID adminId) {
        log.info("Assign university representative request received: userId={}, scopeId={}, adminId={}, expiresAt={}",
                request.getUserId(), request.getScopeId(), adminId, request.getExpiresAt());


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
        log.info("University representative assignment saved: assignmentId={}, userId={}, scopeId={}, adminId={}",
                assignment.getId(), request.getUserId(), request.getScopeId(), adminId);
        int universityRoleId = 2;
        emitRoleAssignment.sendEmittedRoleAssignmentToKafka(request.getUserId(),universityRoleId,request.getScopeId());
        log.info("University representative role emit registered: userId={}, roleId={}, scopeId={}",
                request.getUserId(), universityRoleId, request.getScopeId());
    }


    @Override
    public List<UniversityDashboardDTO> getUniversityDashboard() {
        log.info("University dashboard request received");
        List<University> universities = universityRepository.findAll();
        log.info("University dashboard base data loaded: universityCount={}", universities.size());

        return universities.stream().map(uni -> {
            Optional<RepresentativeAssignment> assignment = representativeRepository
                    .findByScopeIdAndScopeTypeAndIsActiveTrue(uni.getId(), ScopeType.UNIVERSITY);

            String repName = "Assign Representative";

            if (assignment.isPresent()) {
                try {
                    // Call Feign Client
                    repName = userServiceClient.getStudentName(assignment.get().getUserId());
                } catch (Exception e) {
                    // Fallback if User-Service is unreachable or returns an error
                    log.warn("University dashboard representative profile lookup failed: universityId={}, repUserId={}, error={}",
                            uni.getId(), assignment.get().getUserId(), e.getMessage());
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

    @Override
    public GlobalStatsDTO getGlobalStats() {
        log.info("Global stats request received");
        long totalUnis = universityRepository.count();

        // This counts all active reps (University, Dept, Program, etc.)
        long totalReps = representativeRepository.countByIsActiveTrue();
        log.info("Global stats calculated: totalUniversities={}, activeRepresentatives={}", totalUnis, totalReps);

        return new GlobalStatsDTO(totalUnis, totalReps);
    }

    @Override
    public List<RepresentativeDetailsDTO> getAllUniversityReps() {
        log.info("All university representatives request received");
        // 1. Fetch all assignments for UNIVERSITIES
        List<RepresentativeAssignment> assignments = representativeRepository
                .findByScopeType(ScopeType.UNIVERSITY);
        log.info("University representative assignments loaded: assignmentCount={}", assignments.size());

        // 2. Get unique User IDs
        List<UUID> userIds = assignments.stream()
                .map(RepresentativeAssignment::getUserId)
                .distinct()
                .toList();

        if (userIds.isEmpty()) {
            log.info("No university representative users found");
            return Collections.emptyList();
        }

        // 3. Parallel/Bulk Fetch from User Service (Names) and Auth Service (Emails)
        // Fetching Profile Names
        Map<UUID, StudentDTO> userProfileMap = userServiceClient.getBulkUserDetails(userIds);
        log.info("University representative profile details loaded: requestedUsers={}, receivedProfiles={}",
                userIds.size(), userProfileMap.size());

        // Fetching Auth Details (Emails)
        UserBatchRequest batchRequest = new UserBatchRequest(userIds);
        List<UserDetailDTO> authDetailsList = authClient.getUserDetailsBatch(batchRequest);
        log.info("University representative auth details loaded: requestedUsers={}, receivedAuthDetails={}",
                userIds.size(), authDetailsList.size());

        // Map Auth Details by ID for quick lookup
        Map<UUID, UserDetailDTO> authMap = authDetailsList.stream()
                .collect(Collectors.toMap(UserDetailDTO::userId, d -> d));

        // 4. Combine everything
        return assignments.stream().map(rep -> {
            // Find University
            University uni = universityRepository.findById(rep.getScopeId()).orElse(null);

            // Find Profile Name (User Service)
            StudentDTO profile = userProfileMap.get(rep.getUserId());

            // Find Email (Auth Service)
            UserDetailDTO auth = authMap.get(rep.getUserId());

            return new RepresentativeDetailsDTO(
                    rep.getId(),
                    rep.getUserId(),
                    profile != null ? profile.fullName() : "No Profile Created",
                    auth != null ? auth.email() : "Email Not Found",
                    uni != null ? uni.getId() : null,
                    uni != null ? uni.getName() : "Unknown University",
                    uni != null ? uni.getCode() : "N/A",
                    rep.getAssignedAt(),
                    rep.getIsActive()
            );
        }).toList();
    }
}
