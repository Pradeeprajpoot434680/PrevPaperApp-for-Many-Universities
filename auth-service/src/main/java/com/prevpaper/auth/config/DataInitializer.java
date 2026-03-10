package com.prevpaper.auth.config;

import com.prevpaper.auth.entities.Role;
import com.prevpaper.auth.repositories.RoleRepository;

import com.prevpaper.comman.enums.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    public DataInitializer(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public void run(String... args) {
        // Check if roles are already present
        if (roleRepository.count() == 0) {
            Arrays.stream(UserRole.values()).forEach(userRole -> {
                Role role = Role.builder()
                        .roleName(userRole)
                        .hierarchyLevel(userRole.getHierarchyLevel())
                        .build();
                roleRepository.save(role);
            });
            System.out.println("✅ Default roles initialized in the database.");
        }
    }
}