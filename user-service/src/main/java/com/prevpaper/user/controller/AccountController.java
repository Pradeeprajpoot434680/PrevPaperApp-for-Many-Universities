package com.prevpaper.user.controller;

import com.prevpaper.comman.dto.ApiResponse;
import com.prevpaper.user.dto.AcademicUpdateRequest;
import com.prevpaper.user.dto.AccountResponseDTO;
import com.prevpaper.user.entity.Account;
import com.prevpaper.user.entity.User;
import com.prevpaper.user.repository.AccountRepository;
import com.prevpaper.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict; // 🟢 IMPORTED
import org.springframework.cache.annotation.Cacheable; // 🟢 IMPORTED
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
    @Cacheable(value = "userAccounts", key = "#authUserId") // 🟢 READ CACHE
    public ResponseEntity<ApiResponse<AccountResponseDTO>> getMyAccount(@RequestHeader("X-User-Id") String authUserId) {
        log.info("Redis Cache MISS - Loading account details for authUserId={}", authUserId);
        User user = userRepository.findByAuthUserId(UUID.fromString(authUserId))
                .orElseThrow(() -> new RuntimeException("User profile not found"));

        Account account = accountRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Academic account not found"));

        // 🟢 Inside getMyAccount method around Line 40:
        AccountResponseDTO dto = new AccountResponseDTO(
                account.getId(),
                account.getUser().getId(),
                account.getUniversityId(),
                account.getDepartmentId(),
                account.getProgramId(),
                account.getBatchYear() != null ? String.valueOf(account.getBatchYear()) : null, // 🟢 FIXED HERE
                account.getTotalPoints(),
                account.getStatus()
        );
        return ResponseEntity.ok(ApiResponse.success("Account loaded", dto));
    }

    @PatchMapping("/me/academic-info")
    @CacheEvict(value = "userAccounts", key = "#authUserId") // 🟢 EVICT CACHE ON CHANGE
    public ResponseEntity<ApiResponse<AccountResponseDTO>> updateAcademicInfo(
            @RequestHeader("X-User-Id") String authUserId,
            @RequestBody AcademicUpdateRequest request) {
        log.info("Redis Cache EVICT [userAccounts] - Updating academic properties for authUserId={}", authUserId);

        User user = userRepository.findByAuthUserId(UUID.fromString(authUserId)).orElseThrow();
        Account account = accountRepository.findByUserId(user.getId()).orElseThrow();

        account.setUniversityId(request.getUniversityId());
        account.setDepartmentId(request.getDepartmentId());
        account.setBatchYear(request.getBatchYear());

        Account savedAccount = accountRepository.save(account);

        // 🟢 Inside updateAcademicInfo method near the bottom:
        AccountResponseDTO dto = new AccountResponseDTO(
                savedAccount.getId(),
                savedAccount.getUser().getId(),
                savedAccount.getUniversityId(),
                savedAccount.getDepartmentId(),
                savedAccount.getProgramId(),
                savedAccount.getBatchYear() != null ? String.valueOf(savedAccount.getBatchYear()) : null,
                savedAccount.getTotalPoints(),
                savedAccount.getStatus()
        );
        return ResponseEntity.ok(ApiResponse.success("Academic info updated successfully", dto));
    }
}