package com.prevpaper.auth.repositories;

import com.prevpaper.auth.entities.Role;
import com.prevpaper.comman.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Integer> {
    Optional<Role> findByRoleName(UserRole roleName);
}