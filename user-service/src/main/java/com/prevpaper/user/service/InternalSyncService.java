

package com.prevpaper.user.service;

import com.prevpaper.user.dto.UserInternalResponseDTO;
import com.prevpaper.user.dto.UserRequest;
import com.prevpaper.user.dto.UserSyncRequest;

public interface InternalSyncService {
    UserInternalResponseDTO syncNewUser(UserSyncRequest userSyncRequest);
    UserInternalResponseDTO storeUser(UserRequest request, String userId);
}