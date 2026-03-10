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
    public University createUniversity(UniversityRequest request) {
        University university = University.builder()
                .name(request.getName())
                .build();
        return universityRepository.save(university);
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
