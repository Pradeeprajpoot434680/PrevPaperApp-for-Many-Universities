package com.prevpaper.auth.repositories;

import com.prevpaper.auth.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    // For Login and Duplicate Checks
    Optional<User> findByEmail(String email);

    Optional<User> findByPhoneNumber(String phoneNumber);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    // Useful for the future flexible login (Email OR Phone)
    Optional<User> findByEmailOrPhoneNumber(String email, String phoneNumber);
}