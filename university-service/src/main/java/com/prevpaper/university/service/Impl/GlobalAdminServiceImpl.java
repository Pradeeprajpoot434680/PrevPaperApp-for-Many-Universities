package com.prevpaper.university.service.Impl;

import com.prevpaper.comman.enums.ScopeType;
import com.prevpaper.comman.enums.UserRole;
import com.prevpaper.university.dtos.AssignRepRequest;
import com.prevpaper.university.dtos.UniversityRequest;
import com.prevpaper.university.entities.RepresentativeAssignment;
import com.prevpaper.university.entities.University;
import com.prevpaper.university.repository.RepresentativeRepository;
import com.prevpaper.university.repository.UniversityRepository;
import com.prevpaper.university.service.GlobalAdminService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class GlobalAdminServiceImpl implements GlobalAdminService {

    private final UniversityRepository universityRepository;
    private final RepresentativeRepository representativeRepository;

    @Override
    @Transactional
    public University createUniversity(UniversityRequest request) {
        // 1. Correct the check to use the specific field methods
        if (universityRepository.existsByName(request.getName())) {
            throw new RuntimeException("University with name '" + request.getName() + "' already exists");
        }

        if (universityRepository.existsByCode(request.getCode())) {
            throw new RuntimeException("University code '" + request.getCode() + "' is already in use");
        }

        // 2. Build the entity
        University university = University.builder()
                .name(request.getName())
                .code(request.getCode().toUpperCase()) // Store codes in Uppercase (Standard)
                .slug(request.getName().toLowerCase().replaceAll("\\s+", "-")) // Create slug
                .description(request.getDescription())
                .state(request.getState())
                .city(request.getCity())
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
    @Transactional
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
    }
}
