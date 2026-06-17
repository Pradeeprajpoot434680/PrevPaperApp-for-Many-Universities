package com.prevpaper.user.service.Impl;

import com.prevpaper.comman.dto.UserProfileDTO;
import com.prevpaper.comman.exception.ResourceNotFoundException;
import com.prevpaper.user.dto.ProfileUpdateRequest;
import com.prevpaper.user.dto.UserRequest;
import com.prevpaper.user.dto.UserSaveResponseDTO;
import com.prevpaper.user.entity.Account;
import com.prevpaper.user.entity.User;
import com.prevpaper.user.entity.UserPreference;
import com.prevpaper.user.repository.AccountRepository;
import com.prevpaper.user.repository.UserPreferenceRepository;
import com.prevpaper.user.repository.UserRepository;
import com.prevpaper.user.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict; // 🟢 IMPORTED
import org.springframework.cache.annotation.Cacheable; // 🟢 IMPORTED
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Service
@Slf4j
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final UserPreferenceRepository userPreferenceRepository;

    @Override
    public UserSaveResponseDTO createUser(UserRequest request) {
        log.info("Create user called for firstName={} lastName={}", request.getFirstName(), request.getLastName());

        User user = User.builder()
                .authUserId(UUID.randomUUID()) // replace with actual auth ID from context in production
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .build();

        User savedUser = userRepository.save(user);
        log.info("User created successfully. userId={} authUserId={}", savedUser.getId(), savedUser.getAuthUserId());

        return new UserSaveResponseDTO(
                savedUser.getId(),
                savedUser.getAuthUserId(),
                savedUser.getFirstName(),
                savedUser.getLastName(),
                savedUser.getProfileImageUrl(),
                savedUser.getBio()
        );
    }

    /**
     * READ CACHE: Caches the entire composite profile object under the 'userProfiles'
     * namespace, using the authUserId as the primary key tag.
     */
    @Override
    @Transactional
    @Cacheable(value = "userProfiles", key = "#authUserId") // 🟢 READ CACHE ACTIVATED
    public UserProfileDTO getFullProfile(UUID authUserId) {
        log.info("Redis Cache MISS - Computing composite user profile schema from DB for authUserId={}", authUserId);

        Optional<User> userOpt = userRepository.findByAuthUserId(authUserId);
        Optional<Account> accountOpt = userOpt.flatMap(u -> accountRepository.findByUserId(u.getId()));

        String theme = "light";
        String language = "en";
        if (userOpt.isPresent()) {
            UserPreference prefs = userPreferenceRepository.findByUserId(userOpt.get().getId())
                    .orElseGet(() -> userPreferenceRepository.save(UserPreference.builder()
                            .user(userOpt.get())
                            .theme("light")
                            .language("en")
                            .build()));
            theme = prefs.getTheme();
            language = prefs.getLanguage();
        }

        return UserProfileDTO.builder()
                .userId(userOpt.map(User::getId).orElse(null))
                .firstName(userOpt.map(User::getFirstName).orElse(""))
                .lastName(userOpt.map(User::getLastName).orElse(""))
                .profileImageUrl(userOpt.map(User::getProfileImageUrl).orElse(null))
                .bio(userOpt.map(User::getBio).orElse(""))
                .universityId(accountOpt.map(Account::getUniversityId).orElse(null))
                .departmentId(accountOpt.map(Account::getDepartmentId).orElse(null))
                .programId(accountOpt.map(Account::getProgramId).orElse(null))
                .batchYear(accountOpt.map(Account::getBatchYear).orElse(null))
                .totalPoints(accountOpt.map(Account::getTotalPoints).orElse(0L))
                .theme(theme)
                .language(language)
                .build();
    }

    /**
     * MUTATION: Evicts the old cached profile from Redis immediately upon updating values
     * to keep dashboard profile cards synchronized across the app.
     */
    @Override
    @Transactional
    @CacheEvict(value = "userProfiles", key = "#authUserId") // 🟢 EVICS OLD CACHE AUTOMATICALLY
    public UserProfileDTO updateProfile(UUID authUserId, ProfileUpdateRequest request) {
        log.info("Redis Cache EVICT [userProfiles] - Updating profile components for authUserId={}", authUserId);

        User user = userRepository.findByAuthUserId(authUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getBio() != null) user.setBio(request.getBio());
        if (request.getProfileImageUrl() != null) user.setProfileImageUrl(request.getProfileImageUrl());

        User updatedUser = userRepository.save(user);

        // Re-read or map composite account details to return full fresh state securely
        Optional<Account> accountOpt = accountRepository.findByUserId(updatedUser.getId());
        Optional<UserPreference> prefsOpt = userPreferenceRepository.findByUserId(updatedUser.getId());

        return UserProfileDTO.builder()
                .userId(updatedUser.getId())
                .firstName(updatedUser.getFirstName())
                .lastName(updatedUser.getLastName())
                .profileImageUrl(updatedUser.getProfileImageUrl())
                .bio(updatedUser.getBio())
                .universityId(accountOpt.map(Account::getUniversityId).orElse(null))
                .departmentId(accountOpt.map(Account::getDepartmentId).orElse(null))
                .programId(accountOpt.map(Account::getProgramId).orElse(null))
                .batchYear(accountOpt.map(Account::getBatchYear).orElse(null))
                .totalPoints(accountOpt.map(Account::getTotalPoints).orElse(0L))
                .theme(prefsOpt.map(UserPreference::getTheme).orElse("light"))
                .language(prefsOpt.map(UserPreference::getLanguage).orElse("en"))
                .build();
    }
}