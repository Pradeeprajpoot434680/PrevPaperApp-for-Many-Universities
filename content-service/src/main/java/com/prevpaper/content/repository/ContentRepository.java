package com.prevpaper.content.repository;

import com.prevpaper.content.entities.Content;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ContentRepository  extends JpaRepository<Content, UUID> {

}
