package com.prevpaper.user.service;

import com.prevpaper.user.dto.UserRequest;
import com.prevpaper.user.dto.UserSyncRequest;
import com.prevpaper.user.entity.User;

public interface InternalSyncService {
    User syncNewUser(UserSyncRequest userSyncRequest);
    User storeUser(UserRequest userRequest,String userId);
}