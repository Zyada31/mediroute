package com.mediroute.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity @Table(name="user_invites")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UserInvite {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false) private String email;
    @Column(nullable=false) private String role;    // ADMIN, DISPATCHER, DRIVER, PROVIDER
    private Long orgId;
    private Long driverId;

    @Column(nullable=false, unique=true) private String tokenHash;
    @Column(nullable=false) private Instant expiresAt;
    private Instant usedAt;

    private Instant createdAt;
    private Long createdBy;
}