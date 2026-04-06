package com.prevpaper.user.repository;

import com.prevpaper.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByAuthUserId(UUID authUserId);
    List<User> findAllByAuthUserIdIn(List<UUID> userIds);
}