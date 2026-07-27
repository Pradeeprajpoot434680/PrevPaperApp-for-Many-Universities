package com.prevpaper.user.controller;

import com.prevpaper.comman.dto.StudentDTO;
import com.prevpaper.comman.dto.UserData;
import com.prevpaper.comman.producer.StudentEventProducer;
import com.prevpaper.user.dto.UserInternalResponseDTO;
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
    private final StudentEventProducer studentEventProducer;

    public InternalSyncController(InternalSyncService internalSyncService, UserRepository userRepository, StudentEventProducer studentEventProducer) {
        this.internalSyncService = internalSyncService;
        this.userRepository = userRepository;
        this.studentEventProducer = studentEventProducer;
    }

    @PostMapping("/store")
    public ResponseEntity<UserInternalResponseDTO> storeUser(
            @RequestBody UserRequest request,
            @RequestHeader("X-User-Id") String authUserId
    ) {
        UUID userUuid = UUID.fromString(authUserId);
        UUID universityId = UUID.fromString(request.getUniversityId());
        UUID departmentId = UUID.fromString(request.getDepartmentId());
        UUID programId = UUID.fromString(request.getProgramId());
        log.info("Store user request received for authUserId={}", authUserId);
        UserInternalResponseDTO savedUser = internalSyncService.storeUser(request, authUserId);

        studentEventProducer.emitStudentRegistration(
                userUuid,
                universityId,
                departmentId,  // Pass department ID if provided in request
                programId     // Pass program ID if provided in request
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
    }

    @PostMapping("/sync")
    public ResponseEntity<UserInternalResponseDTO> syncNewUser(@RequestBody UserSyncRequest request) {
        log.info("Sync new user request received for authUserId={}", request.getAuthUserId());
        UserInternalResponseDTO savedUser = internalSyncService.syncNewUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
    }

    @PostMapping("/bulk-details")
    public Map<UUID, StudentDTO> getBulkUserDetails(@RequestBody List<UUID> userIds) {
        log.info("USER-SERVICE: Fetching bulk details for {} IDs", userIds.size());

        List<User> users = userRepository.findAllByAuthUserIdIn(userIds);

        return users.stream().collect(Collectors.toMap(
                User::getAuthUserId,
                u -> new StudentDTO(u.getAuthUserId(), u.getFirstName() + " " + u.getLastName(), null)
        ));
    }
//    @PostMapping("/bulk-profiles")
//    public Map<UUID, UserData> getUsersByIds(@RequestBody List<UUID> userIds) {
//        List<User> users = userRepository.findAllById(userIds);
//        return users.stream().collect(Collectors.toMap(
//                User::getId,
//                user -> new UserData(user.getId(), user.getFirstName(), user.getLastName(), user.getProfileImageUrl())
//        ));
//    }

    @PostMapping("/bulk-profiles")
    public Map<UUID, UserData> getUsersByIds(@RequestBody List<UUID> userIds) {
        log.info("USER-SERVICE: Fetching bulk profiles for {} IDs", userIds.size());

        List<User> users = userRepository.findAllByAuthUserIdIn(userIds);

        return users.stream().collect(Collectors.toMap(
                User::getAuthUserId,
                user -> new UserData(
                        user.getAuthUserId(),
                        user.getFirstName(),
                        user.getLastName(),
                        user.getProfileImageUrl()
                )
        ));
    }
}