// src/main/java/com/mediroute/auth/RefreshToken.java
package com.mediroute.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "refresh_tokens",
        indexes = {
                @Index(name="idx_rt_jti", columnList = "jti", unique = true),
                @Index(name="idx_rt_user", columnList = "userId"),
                @Index(name="idx_rt_expires", columnList = "expiresAt")
        })
public class RefreshToken {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String jti; // random ID

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean revoked = false;

    // For rotation & reuse-detection (optional but recommended)
    @Column(length = 64)
    private String rotatedTo; // next jti

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    // Optional audit
    private String userAgent;
    private String ip;

    // getters/setters …
    public Long getId() { return id; }
    public String getJti() { return jti; }
    public void setJti(String jti) { this.jti = jti; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public boolean isRevoked() { return revoked; }
    public void setRevoked(boolean revoked) { this.revoked = revoked; }
    public String getRotatedTo() { return rotatedTo; }
    public void setRotatedTo(String rotatedTo) { this.rotatedTo = rotatedTo; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
}