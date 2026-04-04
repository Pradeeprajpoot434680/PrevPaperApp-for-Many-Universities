package com.prevpaper.user.controller;

import com.prevpaper.user.dto.UserRequest;
import com.prevpaper.user.dto.UserSyncRequest;
import com.prevpaper.user.entity.User;

import com.prevpaper.user.service.InternalSyncService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/users/internal")
public class InternalSyncController {

    private final InternalSyncService internalSyncService;

    public InternalSyncController(InternalSyncService internalSyncService) {
        this.internalSyncService = internalSyncService;
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
}