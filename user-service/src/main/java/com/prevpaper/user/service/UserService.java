package com.prevpaper.user.service;

import com.prevpaper.user.dto.UserRequest;
import com.prevpaper.user.entity.User;

public interface UserService {
    User createUser(UserRequest request);
}
