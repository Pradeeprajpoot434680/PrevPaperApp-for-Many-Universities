package com.prevpaper.user.controller;

import com.prevpaper.user.dto.AcademicUpdateRequest;
import com.prevpaper.user.entity.Account;
import com.prevpaper.user.entity.User;
import com.prevpaper.user.repository.AccountRepository;
import com.prevpaper.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;



@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Slf4j
public class AccountController {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    @GetMapping("/me")
    public ResponseEntity<Account> getMyAccount(@RequestHeader("X-User-Id") String authUserId) {
        log.info("Get account request for authUserId={}", authUserId);
        User user = userRepository.findByAuthUserId(UUID.fromString(authUserId))
                .orElseThrow(() -> new RuntimeException("User profile not found"));

        Account account = accountRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Academic account not found"));

        return ResponseEntity.ok(account);
    }

    @PatchMapping("/me/academic-info")
    public ResponseEntity<Account> updateAcademicInfo(
            @RequestHeader("X-User-Id") String authUserId,
            @RequestBody AcademicUpdateRequest request) {
        log.info("Update academic info request for authUserId={}", authUserId);

        User user = userRepository.findByAuthUserId(UUID.fromString(authUserId)).orElseThrow();
        Account account = accountRepository.findByUserId(user.getId()).orElseThrow();

        account.setUniversityId(request.getUniversityId());
        account.setDepartmentId(request.getDepartmentId());
        account.setBatchYear(request.getBatchYear());

        Account savedAccount = accountRepository.save(account);
        log.info("Academic info updated for authUserId={} accountId={}", authUserId, savedAccount.getId());
        return ResponseEntity.ok(savedAccount);
    }
}
