package com.prevpaper.user.service;

import com.prevpaper.comman.dto.UserProfileDTO;
import com.prevpaper.user.dto.ProfileUpdateRequest;
import com.prevpaper.user.dto.UserRequest;
import com.prevpaper.user.entity.User;

import java.util.UUID;

public interface UserService {
    User createUser(UserRequest request);
    UserProfileDTO getFullProfile(UUID authUserId);
    User updateProfile(UUID authUserId, ProfileUpdateRequest request);
}
