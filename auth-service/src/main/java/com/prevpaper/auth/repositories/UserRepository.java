package com.prevpaper.auth.repositories;

import com.prevpaper.auth.entities.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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



    @Modifying
    @Transactional
    @Query(value = "INSERT INTO user_roles (user_id, role_id) VALUES (:userId, :roleId) ON CONFLICT DO NOTHING", nativeQuery = true)
    void addRoleToUser(@Param("userId") UUID userId, @Param("roleId") Integer roleId);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM user_roles WHERE user_id = :userId AND role_id = :roleId", nativeQuery = true)
    void removeRoleFromUser(@Param("userId") UUID userId, @Param("roleId") Integer roleId);

    @Modifying
    @Transactional
    @Query(value = "UPDATE users SET assigned_scope_id = :scopeId WHERE user_id = :userId", nativeQuery = true)
    void updateUserScope(@Param("userId") UUID userId, @Param("scopeId") UUID scopeId);
}