package com.prevpaper.user.controller;

import com.prevpaper.comman.dto.StudentDTO;
import com.prevpaper.user.entity.Account;
import com.prevpaper.user.repository.AccountRepository;
import com.prevpaper.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable; // 🟢 IMPORTED
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
@Slf4j
public class GetStudents {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    @GetMapping("/department/{deptId}")
    @Cacheable(value = "studentsByDept", key = "#deptId") // 🟢 CACHE DEPT ROSTERS
    public List<StudentDTO> getStudentsByDept(@PathVariable UUID deptId) {
        log.info("Redis Cache MISS - Loading student roster from database for deptId={}", deptId);
        List<Account> accounts = accountRepository.findByDepartmentIdWithProfile(deptId);

        return accounts.stream()
                .map(acc -> new StudentDTO(
                        acc.getUser().getAuthUserId(),
                        acc.getUser().getFirstName() + " " + acc.getUser().getLastName(),
                        null
                )).toList();
    }

    @GetMapping("/program/{programId}")
    @Cacheable(value = "studentsByProg", key = "#programId") // 🟢 CACHE PROGRAM ROSTERS
    public List<StudentDTO> getStudentsByProgram(@PathVariable UUID programId) {
        log.info("Redis Cache MISS - Loading student roster from database for programId={}", programId);
        List<Account> accounts = accountRepository.findByProgramIdWithProfile(programId);

        return accounts.stream()
                .map(acc -> new StudentDTO(
                        acc.getUser().getAuthUserId(),
                        acc.getUser().getFirstName() + " " + acc.getUser().getLastName(),
                        null
                )).toList();
    }

    @GetMapping("/{studentId}")
    @Cacheable(value = "studentNames", key = "#studentId") // 🟢 CACHE INDIVIDUAL LITERAL LOOKUPS
    public String getStudentName(@PathVariable UUID studentId) {
        log.info("Redis Cache MISS - Mapping literal name from DB for studentId={}", studentId);
        return userRepository.findByAuthUserId(studentId)
                .map(user -> user.getFirstName() + " " + user.getLastName())
                .orElse("Profile Pending");
    }
}