package com.prevpaper.user.service;

import com.prevpaper.comman.dto.UserProfileDTO;
import com.prevpaper.user.dto.ProfileUpdateRequest;
import com.prevpaper.user.dto.UserRequest;
import com.prevpaper.user.dto.UserSaveResponseDTO;
import java.util.UUID;

public interface UserService {
    UserSaveResponseDTO createUser(UserRequest request);
    UserProfileDTO getFullProfile(UUID authUserId);
    UserProfileDTO updateProfile(UUID authUserId, ProfileUpdateRequest request); // 🟢 Changed to return DTO
}