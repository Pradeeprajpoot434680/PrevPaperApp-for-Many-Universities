package com.prevpaper.user.service.Impl;

import com.prevpaper.user.dto.UserRequest;
import com.prevpaper.user.entity.User;
import com.prevpaper.user.repository.UserRepository;
import com.prevpaper.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    @Override
    public User createUser(UserRequest request) {
        User user = User.builder()
                .authUserId(UUID.randomUUID()) // replace with actual auth ID
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .build();

        return userRepository.save(user);
    }
}
