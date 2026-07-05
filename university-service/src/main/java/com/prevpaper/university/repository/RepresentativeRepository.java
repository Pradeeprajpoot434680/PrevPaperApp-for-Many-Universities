package com.prevpaper.university.repository;

import com.prevpaper.comman.enums.ScopeType;
import com.prevpaper.university.entities.RepresentativeAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RepresentativeRepository extends JpaRepository<RepresentativeAssignment, UUID> {
    List<RepresentativeAssignment> findByScopeTypeAndIsActiveTrue(ScopeType scopeType);
    Optional<RepresentativeAssignment>  findByScopeIdAndScopeTypeAndIsActiveTrue(UUID id, ScopeType type);
    List<RepresentativeAssignment>      findByScopeIdInAndScopeTypeAndIsActiveTrue(List<UUID> programId, ScopeType type);

    // Count only active assignments
    long countByIsActiveTrue();

    // If you want specifically University Reps only:
    long countByScopeTypeAndIsActiveTrue(ScopeType scopeType);

    List<RepresentativeAssignment> findByScopeType(ScopeType type);

    // Fetches every rep linked to any of the collected IDs (Uni, Dept, Prog, or Session)
    List<RepresentativeAssignment> findAllByScopeIdInAndIsActiveTrue(List<UUID> scopeIds);

    boolean existsByUserIdAndScopeIdAndIsActiveTrue(UUID userId, UUID scopeId);
}
