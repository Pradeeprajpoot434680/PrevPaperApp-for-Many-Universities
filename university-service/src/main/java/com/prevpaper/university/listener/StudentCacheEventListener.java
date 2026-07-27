package com.prevpaper.university.listener;

import com.prevpaper.comman.dto.StudentRegisteredEvent;
import com.prevpaper.university.service.CacheEvictionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StudentCacheEventListener {

    private final CacheEvictionService cacheEvictionService;

    @KafkaListener(
            topics = "student-events",
            groupId = "university-cache-eviction-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onStudentRegistered(StudentRegisteredEvent event) {
        log.info("🔥 Received StudentRegisteredEvent for userId={}. Evicting Redis caches...", event.userId());

        if (event.universityId() != null) {
            cacheEvictionService.evictUniversityReps(event.universityId());
        }
        if (event.departmentId() != null) {
            cacheEvictionService.evictDepartmentStudents(event.departmentId());
            cacheEvictionService.evictDepartmentReps(event.departmentId());
            cacheEvictionService.evictDepartmentDashboard(event.departmentId());
        }
        if (event.programId() != null) {
            cacheEvictionService.evictProgramStudents(event.programId());
            cacheEvictionService.evictProgramSessions(event.programId());
        }
    }
}