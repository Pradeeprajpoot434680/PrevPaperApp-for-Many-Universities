package com.prevpaper.user.controller;

import com.prevpaper.comman.dto.StudentDTO;
import com.prevpaper.comman.exception.ResourceNotFoundException;
import com.prevpaper.user.entity.Account;
import com.prevpaper.user.entity.User;
import com.prevpaper.user.repository.AccountRepository;
import com.prevpaper.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
public class GetStudents {


    private final AccountRepository accountRepository;
    private final UserRepository userRepository;


    @GetMapping("/department/{deptId}")
    public List<StudentDTO> getStudentsByDept(@PathVariable UUID deptId) {
        List<Account> accounts = accountRepository.findByDepartmentIdWithProfile(deptId);

        return accounts.stream()
                .map(acc -> new StudentDTO(
                        acc.getUser().getAuthUserId(),
                        acc.getUser().getFirstName() + " " + acc.getUser().getLastName(),
                        null // Email is in Auth-Service; we will fetch it if needed or add it to Account later
                ))
                .toList();
    }

    @GetMapping("/program/{programId}")
    public List<StudentDTO> getStudentsByProgram(@PathVariable UUID programId) {
        List<Account> accounts = accountRepository.findByProgramIdWithProfile(programId);

        return accounts.stream()
                .map(acc -> new StudentDTO(
                        acc.getUser().getAuthUserId(),
                        acc.getUser().getFirstName() + " " + acc.getUser().getLastName(),
                        null // Email is in Auth-Service; we will fetch it if needed or add it to Account later
                ))
                .toList();
    }

    @GetMapping("/{studentId}")
    public String getStudentName(@PathVariable UUID studentId) {
        // Look for the profile using authUserId
        return userRepository.findByAuthUserId(studentId)
                .map(user -> user.getFirstName() + " " + user.getLastName())
                .orElse("Profile Pending"); // Return this instead of throwing an error
    }

}
