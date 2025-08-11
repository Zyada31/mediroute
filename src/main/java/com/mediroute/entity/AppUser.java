// src/main/java/com/mediroute/auth/AppUser.java
package com.mediroute.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "app_users",
        indexes = {
                @Index(name="idx_users_email", columnList = "email", unique = true),
                @Index(name="idx_users_active", columnList = "active")
        })
public class AppUser {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank @Email
    @Column(nullable = false, unique = true, length = 160)
    private String email;

    @NotBlank
    @Column(nullable = false, length = 100) // BCrypt hash length ~60
    private String passwordHash;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role", nullable = false, length = 40)
    private List<String> roles = new ArrayList<>();

    // If a DRIVER account maps to a driver record in your domain
    private Long driverId;

    @Column(nullable = false)
    private boolean active = true;

    // getters/setters …
    public Long getId() { return id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles; }
    public Long getDriverId() { return driverId; }
    public void setDriverId(Long driverId) { this.driverId = driverId; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}