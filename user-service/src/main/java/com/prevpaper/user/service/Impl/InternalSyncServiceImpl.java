package com.prevpaper.user.service.Impl;

import com.prevpaper.comman.enums.AccountStatus;
import com.prevpaper.user.client.AuthServiceClient;
import com.prevpaper.user.dto.UserInternalResponseDTO;
import com.prevpaper.user.dto.UserRequest;
import com.prevpaper.user.dto.UserSyncRequest;
import com.prevpaper.user.entity.Account;
import com.prevpaper.user.entity.User;
import com.prevpaper.user.entity.UserPreference;
import com.prevpaper.user.repository.AccountRepository;
import com.prevpaper.user.repository.UserPreferenceRepository;
import com.prevpaper.user.repository.UserRepository;
import com.prevpaper.user.service.InternalSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict; // 🟢 IMPORTED
import org.springframework.cache.annotation.Caching;    // 🟢 IMPORTED
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
@Slf4j
public class InternalSyncServiceImpl implements InternalSyncService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final UserPreferenceRepository preferenceRepository;
    private final AuthServiceClient authServiceClient;

    public InternalSyncServiceImpl(UserRepository userRepository, AccountRepository accountRepository, UserPreferenceRepository preferenceRepository, AuthServiceClient authServiceClient) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.preferenceRepository = preferenceRepository;
        this.authServiceClient = authServiceClient;
    }

    @Override
    public UserInternalResponseDTO syncNewUser(UserSyncRequest userSyncRequest) {
        log.info("Sync user process started for authUserId={}", userSyncRequest.getAuthUserId());

        User savedUser = userRepository.findByAuthUserId(userSyncRequest.getAuthUserId())
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .authUserId(userSyncRequest.getAuthUserId())
                            .firstName(userSyncRequest.getFirstName())
                            .lastName(userSyncRequest.getLastName())
                            .build();

                    User persistedUser = userRepository.save(newUser);

                    Account newAccount = Account.builder()
                            .user(persistedUser)
                            .totalPoints(0L)
                            .status(AccountStatus.ACTIVE)
                            .build();
                    accountRepository.save(newAccount);

                    preferenceRepository.save(UserPreference.builder()
                            .user(persistedUser)
                            .theme("light")
                            .language("en")
                            .build());

                    return persistedUser;
                });

        return new UserInternalResponseDTO(savedUser.getId(), savedUser.getAuthUserId(), savedUser.getFirstName(), savedUser.getLastName(), savedUser.getProfileImageUrl(), savedUser.getBio());
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "studentsByDept", key = "#request.departmentId"),
            @CacheEvict(value = "studentsByProg", key = "#request.programId"),
            @CacheEvict(value = "studentNames", key = "#userId")
    }) // 🟢 FLUSHES ALL RELATED READ CACHES FOR REPS & UNIVERSITY STRUCTURES
    public UserInternalResponseDTO storeUser(UserRequest request, String userId) {
        UUID authUserId = UUID.fromString(userId);
        log.info("Redis Cache EVICT [studentsByDept, studentsByProg, studentNames] - Storing user context: {}", authUserId);

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .authUserId(authUserId)
                .build();

        User savedUser = userRepository.save(user);

        Account account = Account.builder()
                .user(savedUser)
                .universityId(UUID.fromString(request.getUniversityId()))
                .departmentId(UUID.fromString(request.getDepartmentId()))
                .programId(UUID.fromString(request.getProgramId()))
                .batchYear(request.getBatchYear())
                .totalPoints(0L)
                .status(AccountStatus.ACTIVE)
                .build();

        accountRepository.save(account);

        String fullName = request.getFirstName() + " " + request.getLastName();
        authServiceClient.setFullName(authUserId, fullName);

        return new UserInternalResponseDTO(savedUser.getId(), savedUser.getAuthUserId(), savedUser.getFirstName(), savedUser.getLastName(), savedUser.getProfileImageUrl(), savedUser.getBio());
    }
}