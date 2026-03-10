package com.prevpaper.auth.repositories;

import com.prevpaper.auth.entities.User;
import com.prevpaper.auth.entities.VerificationToken;
import com.prevpaper.comman.enums.TokenType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {
    Optional<VerificationToken> findByTokenAndType(String token, TokenType type);
    void deleteByUserAndType(User user, TokenType type);
    Optional<VerificationToken> findByUser(User user);
}
