package com.prevpaper.user.repository;

import com.prevpaper.user.entity.User;
import com.prevpaper.user.entity.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserPreferenceRepository extends JpaRepository<UserPreference, UUID> {
    // Ensure the method name matches the property in the Entity (user.id)
    Optional<UserPreference> findByUserId(UUID userId);
}