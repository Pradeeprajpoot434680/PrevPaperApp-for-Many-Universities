package com.prevpaper.user.controller;

import com.prevpaper.comman.dto.StudentDTO;
import com.prevpaper.user.entity.Account;
import com.prevpaper.user.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
public class GetStudents {


    private final AccountRepository accountRepository;

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
}
