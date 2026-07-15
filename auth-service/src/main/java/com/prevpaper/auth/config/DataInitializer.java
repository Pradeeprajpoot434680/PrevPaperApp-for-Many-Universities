package com.prevpaper.auth.config;

import com.prevpaper.auth.entities.Role;
import com.prevpaper.auth.entities.User;
import com.prevpaper.auth.repositories.RoleRepository;
import com.prevpaper.auth.repositories.UserRepository;
import com.prevpaper.comman.enums.AccountStatus;
import com.prevpaper.comman.enums.AuthProvider;
import com.prevpaper.comman.enums.UserRole;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // 🟢 Constructor injection for all required security beans
    public DataInitializer(RoleRepository roleRepository,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional // Ensures user-role mapping transaction safety
    public void run(String... args) {
        // 1. Check if roles are already present, initialize if empty
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

        // 2. 🟢 Create the Global Admin automatically if it doesn't exist
        String adminEmail = "pradeep@gmail.com";
        if (!userRepository.existsByEmail(adminEmail)) {
            Role adminRole = roleRepository.findByRoleName(UserRole.GLOBAL_ADMIN)
                    .orElseThrow(() -> new RuntimeException("GLOBAL_ADMIN role missing in context setup"));

            User adminUser = User.builder()
                    .email(adminEmail)
                    .fullName("Supreme Global Admin")
                    .phoneNumber("8650152081")
                    // 🟢 Automatically hashes the password using your exact active BCrypt encoder instance
                    .passwordHash(passwordEncoder.encode("Admin@123"))
                    .provider(AuthProvider.INTERNAL)
                    .accountStatus(AccountStatus.ACTIVE)
                    .roles(Set.of(adminRole))
                    .build();

            userRepository.save(adminUser);
            System.out.println("System initialized with default GLOBAL_ADMIN user: " + adminEmail);
        }
    }
}