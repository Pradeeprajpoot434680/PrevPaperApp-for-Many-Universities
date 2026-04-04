package com.prevpaper.university.repository;

import com.prevpaper.comman.enums.ScopeType;
import com.prevpaper.university.entities.RepresentativeAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RepresentativeRepository extends JpaRepository<RepresentativeAssignment, UUID> {
    List<RepresentativeAssignment> findByScopeTypeAndIsActiveTrue(ScopeType scopeType);
}
