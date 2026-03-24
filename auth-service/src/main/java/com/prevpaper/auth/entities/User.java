package com.prevpaper.auth.entities;

import com.prevpaper.comman.enums.AccountStatus;
import com.prevpaper.comman.enums.AuthProvider;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID userId;

    @Column(unique = true)
    private String email;

    private String passwordHash; // Nullable for Google OAuth

    private String fullName;

    private UUID universityId; // Reference for multi-tenant isolation

    @Enumerated(EnumType.STRING)
    private AuthProvider provider; // INTERNAL, GOOGLE

    private String providerId; // OAuth 'sub' claim

    @Enumerated(EnumType.STRING)
    private AccountStatus accountStatus; // PENDING, ACTIVE, BANNED

    @Column(unique = true)
    private String phoneNumber;


    private UUID assignedScopeId;


    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.getRoleName().name()))
                .collect(Collectors.toList());
    }

    @Override
    public String getPassword() {
        return this.passwordHash;
    }

    @Override
    public String getUsername() {
        return this.email; // or this.phoneNumber if you prefer
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() {
        return this.accountStatus != AccountStatus.BANNED;
    }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() {
        return this.accountStatus == AccountStatus.ACTIVE;
    }
}