package com.prevpaper.user.controller;

import com.prevpaper.comman.dto.StudentDTO;
import com.prevpaper.comman.dto.UserData;
import com.prevpaper.user.dto.UserRequest;
import com.prevpaper.user.dto.UserSyncRequest;
import com.prevpaper.user.entity.User;

import com.prevpaper.user.repository.UserRepository;
import com.prevpaper.user.service.InternalSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/api/v1/users/internal")
@Slf4j
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
        log.info("Store user request received for authUserId={}", authUserId);
        User savedUser = internalSyncService.storeUser(request, authUserId);
        log.info("User stored successfully. userId={} authUserId={}", savedUser.getId(), savedUser.getAuthUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
    }

    @PostMapping("/sync")
    public ResponseEntity<User> syncNewUser(@RequestBody UserSyncRequest request) {
        log.info("Sync new user request received for authUserId={}", request.getAuthUserId());
        User savedUser = internalSyncService.syncNewUser(request);
        log.info("User sync completed. userId={} authUserId={}", savedUser.getId(), savedUser.getAuthUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
    }

    @GetMapping("/check")
    public String check(){
        return "Checking is well";
    }



    @PostMapping("/bulk-details")
    public Map<UUID, StudentDTO> getBulkUserDetails(@RequestBody List<UUID> userIds) {
        log.debug("Bulk details requested for {} users", userIds.size());
        List<User> users = userRepository.findAllByAuthUserIdIn(userIds);
        return users.stream().collect(Collectors.toMap(
                User::getAuthUserId,
                u -> new StudentDTO(u.getAuthUserId(), u.getFirstName() + " " + u.getLastName(), null)
        ));
    }


    @PostMapping("/bulk-profiles")
    public Map<UUID, UserData> getUsersByIds(@RequestBody List<UUID> userIds) {
        log.debug("Bulk profiles requested for {} users", userIds.size());
        List<User> users = userRepository.findAllById(userIds);

        return users.stream().collect(Collectors.toMap(
                User::getId,
                user -> new UserData(
                        user.getId(),
                        user.getFirstName(),
                        user.getLastName(),
                        user.getProfileImageUrl()
                )
        ));
    }
}
