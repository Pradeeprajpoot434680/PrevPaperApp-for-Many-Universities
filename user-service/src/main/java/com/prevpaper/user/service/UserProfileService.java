package com.prevpaper.user.service;

import com.prevpaper.user.dto.ProfileUpdateRequest;
import com.prevpaper.user.entity.User;
import com.prevpaper.user.entity.UserPreference;

import java.util.UUID;

public interface UserProfileService {
    User updateProfile(UUID authUserId, ProfileUpdateRequest request);
    UserPreference getPreferences(UUID authUserId);
}
