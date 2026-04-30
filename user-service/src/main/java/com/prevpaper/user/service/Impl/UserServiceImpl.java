package com.prevpaper.user.service.Impl;

import com.prevpaper.comman.dto.UserProfileDTO;
import com.prevpaper.comman.exception.ResourceNotFoundException;
import com.prevpaper.user.dto.ProfileUpdateRequest;
import com.prevpaper.user.dto.UserRequest;
import com.prevpaper.user.entity.Account;
import com.prevpaper.user.entity.User;
import com.prevpaper.user.entity.UserPreference;
import com.prevpaper.user.repository.AccountRepository;
import com.prevpaper.user.repository.UserPreferenceRepository;
import com.prevpaper.user.repository.UserRepository;
import com.prevpaper.user.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private  final AccountRepository accountRepository;
    private final UserPreferenceRepository userPreferenceRepository;

    @Override
    public User createUser(UserRequest request) {
        User user = User.builder()
                .authUserId(UUID.randomUUID()) // replace with actual auth ID
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .build();

        return userRepository.save(user);
    }

    @Override
    @Transactional
    public UserProfileDTO getFullProfile(UUID authUserId) {
        // 1. Fetch User Profile (if not present, we use a null-safe approach)
        Optional<User> userOpt = userRepository.findByAuthUserId(authUserId);

        // 2. Fetch Academic Account linked to the user
        Optional<Account> accountOpt = userOpt.flatMap(u -> accountRepository.findByUserId(u.getId()));

        // 3. Handle Preferences (only if user exists)
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

        // 4. Build Aggregated DTO with Null-Safety
        return UserProfileDTO.builder()
                // Profile data (if user exists, use it; else null)
                .userId(userOpt.map(User::getId).orElse(null))
                .firstName(userOpt.map(User::getFirstName).orElse(""))
                .lastName(userOpt.map(User::getLastName).orElse(""))
                .profileImageUrl(userOpt.map(User::getProfileImageUrl).orElse(null))
                .bio(userOpt.map(User::getBio).orElse(""))

                // Account data (if account exists, use it; else null/0)
                .universityId(accountOpt.map(Account::getUniversityId).orElse(null))
                .departmentId(accountOpt.map(Account::getDepartmentId).orElse(null))
                .programId(accountOpt.map(Account::getProgramId).orElse(null))
                .batchYear(accountOpt.map(Account::getBatchYear).orElse(null))
                .totalPoints(accountOpt.map(Account::getTotalPoints).orElse(0L))

                // Preferences
                .theme(theme)
                .language(language)
                .build();
    }

    @Override
    @Transactional
    public User updateProfile(UUID authUserId, ProfileUpdateRequest request) {
        User user = userRepository.findByAuthUserId(authUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getBio() != null) user.setBio(request.getBio());
        if (request.getProfileImageUrl() != null) user.setProfileImageUrl(request.getProfileImageUrl());

        return userRepository.save(user);
    }
}
