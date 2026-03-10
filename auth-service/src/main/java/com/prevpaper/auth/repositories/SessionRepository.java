package com.prevpaper.auth.repositories;

import com.prevpaper.auth.entities.Session;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SessionRepository extends JpaRepository<Session, UUID> {

}
