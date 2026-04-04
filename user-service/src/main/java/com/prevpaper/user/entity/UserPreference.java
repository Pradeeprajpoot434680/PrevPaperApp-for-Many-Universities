package com.prevpaper.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;




@Entity
@Table(name = "user_preferences")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UserPreference {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String language = "en";
    private String theme = "light";
    private Boolean emailNotifications = true;
}