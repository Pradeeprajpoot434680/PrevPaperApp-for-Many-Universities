package com.prevpaper.user.service.Impl;

import com.prevpaper.comman.enums.AccountStatus;
import com.prevpaper.user.client.AuthServiceClient;
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
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
public class InternalSyncServiceImpl implements InternalSyncService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final UserPreferenceRepository preferenceRepository;
    private  final AuthServiceClient authServiceClient;

    public InternalSyncServiceImpl(UserRepository userRepository, AccountRepository accountRepository, UserPreferenceRepository preferenceRepository, AuthServiceClient authServiceClient) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.preferenceRepository = preferenceRepository;
        this.authServiceClient = authServiceClient;
    }
    

    @Override
    public User syncNewUser(UserSyncRequest userSyncRequest) {
        log.info("Sync user process started for authUserId={}", userSyncRequest.getAuthUserId());

        // 1. Check if user already exists
        return userRepository.findByAuthUserId(userSyncRequest.getAuthUserId())
                .orElseGet(() -> {

                    // 2. Create User
                    User newUser = User.builder()
                            .authUserId(userSyncRequest.getAuthUserId())
                            .firstName(userSyncRequest.getFirstName())
                            .lastName(userSyncRequest.getLastName())
                            .build();

                    User savedUser = userRepository.save(newUser);

                    // 3. Create Account
                    Account newAccount = Account.builder()
                            .user(savedUser)
                            .totalPoints(0L)
                            .status(AccountStatus.ACTIVE)
                            .build();
                    accountRepository.save(newAccount);

                    // 4. Create Preferences
                    preferenceRepository.save(UserPreference.builder()
                            .user(savedUser)
                            .theme("light")
                            .language("en")
                            .build());

                    log.info("New user synced successfully. userId={} authUserId={}", savedUser.getId(), savedUser.getAuthUserId());
                    return savedUser;
                });
    }

    @Override
    public User storeUser(UserRequest request,String userId) {

        UUID authUserId = UUID.fromString(userId);
        log.info("Store user process started for authUserId={}", authUserId);
        // 1️⃣ Create User
        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .authUserId(authUserId)
                .build();


        log.debug("Prepared user entity for persistence: {}", user);

        User savedUser = userRepository.save(user);

        Account account = Account.builder()
                .user(savedUser) // IMPORTANT: relation
                .universityId(UUID.fromString(request.getUniversityId()))
                .departmentId(UUID.fromString(request.getDepartmentId()))
                .programId(UUID.fromString(request.getProgramId()))// FIXED
                .batchYear(request.getBatchYear())
                .build();

        accountRepository.save(account);
        log.info("User and account saved successfully. userId={} accountId={}", savedUser.getId(), account.getId());
        // set the full name in the auth service
        String fullName = request.getFirstName() + " "  +request.getLastName();

        log.debug("Calling auth service to set full name for authUserId={}", authUserId);
        authServiceClient.setFullName(authUserId,fullName);

        log.info("Store user process completed for authUserId={}", authUserId);
        return savedUser;
    }
}
