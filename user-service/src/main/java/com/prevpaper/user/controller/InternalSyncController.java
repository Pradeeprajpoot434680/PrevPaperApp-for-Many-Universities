package com.prevpaper.user.controller;

import com.prevpaper.comman.dto.StudentDTO;
import com.prevpaper.user.dto.UserRequest;
import com.prevpaper.user.dto.UserSyncRequest;
import com.prevpaper.user.entity.User;

import com.prevpaper.user.repository.UserRepository;
import com.prevpaper.user.service.InternalSyncService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/api/v1/users/internal")
public class InternalSyncController {

    private final InternalSyncService internalSyncService;
    private final UserRepository userRepository;

    public InternalSyncController(InternalSyncService internalSyncService, UserRepository userRepository) {
        this.internalSyncService = internalSyncService;
        this.userRepository = userRepository;
    }

    @PostMapping("/store")
    public ResponseEntity<User> storeUser(
            @RequestBody UserRequest request,
            @RequestHeader("X-User-Id") String authUserId // Get ID from Gateway header
    ) {
        User savedUser = internalSyncService.storeUser(request, authUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
    }

    @PostMapping("/sync")
    public ResponseEntity<User> syncNewUser(@RequestBody UserSyncRequest request) {
        User savedUser = internalSyncService.syncNewUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
    }

    @GetMapping("/check")
    public String check(){
        return "Checking is well";
    }



    @PostMapping("/bulk-details")
    public Map<UUID, StudentDTO> getBulkUserDetails(@RequestBody List<UUID> userIds) {
        List<User> users = userRepository.findAllByAuthUserIdIn(userIds);
        return users.stream().collect(Collectors.toMap(
                User::getAuthUserId,
                u -> new StudentDTO(u.getAuthUserId(), u.getFirstName() + " " + u.getLastName(), null)
        ));
    }
}