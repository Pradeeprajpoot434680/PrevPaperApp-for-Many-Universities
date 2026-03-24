package com.prevpaper.auth.consumer;

import com.prevpaper.auth.repositories.UserRepository;
import com.prevpaper.comman.dto.RoleChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
@Service
@RequiredArgsConstructor
@Slf4j
public class RoleChangeConsumer {

    private final UserRepository userRepository;

    @KafkaListener(topics = "role-change-events", groupId = "auth-role-group")
    public void consumeRoleChange(RoleChangedEvent event) {
        log.info("Processing role change: User={} Role={} Scope={}",
                event.userId(), event.roleId(), event.scopeId());

        try {
            if ("ASSIGN".equals(event.action())) {
                // 1. Assign Role in Join Table
                userRepository.addRoleToUser(event.userId(), event.roleId());

                // 2. Assign Scope ID in Users Table
                // You must define this method in UserRepository using @Modifying
                userRepository.updateUserScope(event.userId(), event.scopeId());

                log.info("Role and Scope updated for user: {}", event.userId());
            }
        } catch (Exception e) {
            log.error("Database error during role sync: {}", e.getMessage());
        }
    }
}