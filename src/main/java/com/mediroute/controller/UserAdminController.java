// src/main/java/com/mediroute/controller/UserAdminController.java
package com.mediroute.controller;

import com.mediroute.dto.InviteCreateReq;
import com.mediroute.dto.InviteCreateRes;
import com.mediroute.entity.UserInvite;
import com.mediroute.repository.UserInviteRepo;
import com.mediroute.repository.UserRepo;
import com.mediroute.config.AppProps;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
public class UserAdminController {
    private final UserInviteRepo invites;
    private final UserRepo users;
    private final AppProps props;

    @PostMapping("/invite")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')") // or ADMIN only
    public ResponseEntity<InviteCreateRes> invite(@RequestBody InviteCreateReq req) {
        // idempotency-ish: if user exists and active, abort
        users.findByEmail(req.getEmail()).ifPresent(u -> {
            if (u.isActive()) throw new IllegalStateException("User already active");
        });

        String rawToken = UUID.randomUUID().toString().replace("-", "");
        String tokenHash = DigestUtils.md5DigestAsHex(rawToken.getBytes(StandardCharsets.UTF_8));

        UserInvite inv = UserInvite.builder()
                .email(req.getEmail())
                .role(req.getRole())
                .orgId(req.getOrgId())
                .driverId(req.getDriverId())
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .createdAt(Instant.now())
                .build();
        invites.save(inv);

        String token = rawToken + "." + tokenHash.substring(0, 8); // simple obfuscation
        String base = props.getSecurity().getIssuer(); // e.g., https://mediroute.local
        String url = base + "/activate?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);

        // TODO: send via email/SMS
        return ResponseEntity.ok(new InviteCreateRes(url));
    }
}