package com.prevpaper.university.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
public class CacheEvictionService {

    @CacheEvict(value = "universityReps", key = "#universityId")
    public void evictUniversityReps(UUID universityId) {
        log.info("🧹 Flushed 'universityReps' cache for universityId={}", universityId);
    }

    @CacheEvict(value = "deptStudents", key = "#deptId")
    public void evictDepartmentStudents(UUID deptId) {
        log.info("🧹 Flushed 'deptStudents' cache for departmentId={}", deptId);
    }

    @CacheEvict(value = "programReps", key = "#deptId")
    public void evictDepartmentReps(UUID deptId) {
        log.info("🧹 Flushed 'programReps' cache for departmentId={}", deptId);
    }

    @CacheEvict(value = "departmentDashboards", key = "#deptId")
    public void evictDepartmentDashboard(UUID deptId) {
        log.info("🧹 Flushed 'departmentDashboards' cache for departmentId={}", deptId);
    }

    @CacheEvict(value = "programStudents", key = "#programId")
    public void evictProgramStudents(UUID programId) {
        log.info("🧹 Flushed 'programStudents' cache for programId={}", programId);
    }

    @CacheEvict(value = "programSessions", key = "#programId")
    public void evictProgramSessions(UUID programId) {
        log.info("🧹 Flushed 'programSessions' cache for programId={}", programId);
    }
}