package com.prevpaper.user.service.Impl;

import com.prevpaper.comman.exception.ResourceNotFoundException;
import com.prevpaper.user.dto.ProfileUpdateRequest;
import com.prevpaper.user.entity.User;
import com.prevpaper.user.entity.UserPreference;
import com.prevpaper.user.repository.UserPreferenceRepository;
import com.prevpaper.user.repository.UserRepository;
import com.prevpaper.user.service.UserProfileService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UserRepository userRepository;
    private final UserPreferenceRepository preferenceRepository;

    @Transactional
    public User updateProfile(UUID authUserId, ProfileUpdateRequest request) {
        User user = userRepository.findByAuthUserId(authUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setBio(request.getBio());
        user.setProfileImageUrl(request.getProfileImageUrl());

        return userRepository.save(user);
    }

    public UserPreference getPreferences(UUID authUserId) {
        User user = userRepository.findByAuthUserId(authUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Use the corrected repository method
        return preferenceRepository.findByUserId(user.getId())
                .orElseGet(() -> createDefaultPreference(user));
    }

    private UserPreference createDefaultPreference(User user) {
        return preferenceRepository.save(UserPreference.builder()
                .user(user)
                .theme("light")
                .language("en")
                .build());
    }
}