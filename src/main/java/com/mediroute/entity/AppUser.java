// src/main/java/com/mediroute/auth/AppUser.java
package com.mediroute.entity;

import jakarta.persistence.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Entity
@Table(name = "app_users",
        indexes = {
                @Index(name="idx_users_email", columnList = "email", unique = true),
                @Index(name="idx_users_active", columnList = "active")
        })
public class AppUser {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    private boolean active;

    /** Simple storage for now: "ADMIN,DISPATCHER" */
    @Column(columnDefinition = "text", nullable = false)
    private String roles;

    @Column(name = "driver_id")
    private Long driverId;

    @Column(name = "mfa_enabled")
    private Boolean mfaEnabled;

    @Column(name = "mfa_totp_secret")
    private String mfaTotpSecret;

    // getters/setters …
    public Long getId() { return id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public List<String> getRoles() { return Collections.singletonList(roles); }
    public void setRoles(String roles) { this.roles = roles; }
    public Long getDriverId() { return driverId; }
    public void setDriverId(Long driverId) { this.driverId = driverId; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    // keep JPA backing string
    public String getRolesRaw() { return roles; } // optional convenience

    /** Use this in app logic; yields ["ADMIN","DISPATCHER"] instead of ["ADMIN,DISPATCHER"]. */
    public List<String> getRoleList() {
        if (roles == null || roles.isBlank()) return List.of();
        return Arrays.stream(roles.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();
    }

    /** Optional: allow setting from list while persisting CSV. */
    public void setRoleList(List<String> roleList) {
        this.roles = (roleList == null ? "" :
                roleList.stream()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .distinct()
                        .collect(Collectors.joining(",")));
    }

    public void setOrgId(Long orgId) {

    }

    // MFA getters/setters
    public Boolean getMfaEnabled() { return mfaEnabled; }
    public boolean isMfaEnabled() { return Boolean.TRUE.equals(mfaEnabled); }
    public void setMfaEnabled(boolean enabled) { this.mfaEnabled = enabled; }
    public String getMfaTotpSecret() { return mfaTotpSecret; }
    public void setMfaTotpSecret(String mfaTotpSecret) { this.mfaTotpSecret = mfaTotpSecret; }
}