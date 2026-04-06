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
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
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
        // 1. Validation checks
        if (universityRepository.existsByName(request.getName())) {
            throw new RuntimeException("University with name '" + request.getName() + "' already exists");
        }

        if (universityRepository.existsByCode(request.getCode())) {
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

        return universityRepository.save(university);
    }
    private String generateSlug(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "") // remove special chars
                .replaceAll("\\s+", "-");        // replace spaces with hyphens
    }

    @Override

    public void assignUniversityRep(AssignRepRequest request, UUID adminId) {


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
        emitRoleAssignment.sendEmittedRoleAssignmentToKafka(request.getUserId(),universityRoleId,request.getScopeId());
    }


    @Override
    public List<UniversityDashboardDTO> getUniversityDashboard() {
        List<University> universities = universityRepository.findAll();

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
        long totalUnis = universityRepository.count();

        // This counts all active reps (University, Dept, Program, etc.)
        long totalReps = representativeRepository.countByIsActiveTrue();

        return new GlobalStatsDTO(totalUnis, totalReps);
    }

    @Override
    public List<RepresentativeDetailsDTO> getAllUniversityReps() {
        // 1. Fetch all assignments for UNIVERSITIES
        List<RepresentativeAssignment> assignments = representativeRepository
                .findByScopeType(ScopeType.UNIVERSITY);

        // 2. Get unique User IDs
        List<UUID> userIds = assignments.stream()
                .map(RepresentativeAssignment::getUserId)
                .distinct()
                .toList();

        if (userIds.isEmpty()) return Collections.emptyList();

        // 3. Parallel/Bulk Fetch from User Service (Names) and Auth Service (Emails)
        // Fetching Profile Names
        Map<UUID, StudentDTO> userProfileMap = userServiceClient.getBulkUserDetails(userIds);

        // Fetching Auth Details (Emails)
        UserBatchRequest batchRequest = new UserBatchRequest(userIds);
        List<UserDetailDTO> authDetailsList = authClient.getUserDetailsBatch(batchRequest);

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
