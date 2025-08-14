package com.mediroute.controller;

import com.mediroute.config.AppProps;
import com.mediroute.dto.ActivateReq;
import com.mediroute.entity.AppUser;
import com.mediroute.entity.RefreshToken;
import com.mediroute.entity.UserInvite;
import com.mediroute.repository.RefreshTokenRepo;
import com.mediroute.repository.UserInviteRepo;
import com.mediroute.repository.UserRepo;
import com.mediroute.service.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.mediroute.service.security.TotpService.validateCode;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final UserRepo userRepo;
    private final RefreshTokenRepo rtRepo;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final AppProps props;
    private final UserInviteRepo inviteRepo;

    public AuthController(UserRepo userRepo, RefreshTokenRepo rtRepo,
                          PasswordEncoder encoder, JwtService jwt, AppProps props,
                          UserInviteRepo inviteRepo) {
        this.userRepo = userRepo; this.rtRepo = rtRepo; this.encoder = encoder; this.jwt = jwt; this.props = props;
        this.inviteRepo = inviteRepo;
    }

    @PostMapping("/activate")
    public ResponseEntity<?> activate(@RequestBody @Valid ActivateReq req) {
        // Parse token -> tokenHash
        String[] parts = req.getToken().split("\\.");
        if (parts.length != 2) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid token");
        String raw = parts[0];
        String suffix = parts[1];

        String tokenHash = org.springframework.util.DigestUtils
                .md5DigestAsHex(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        if (!tokenHash.startsWith(suffix)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid token");

        UserInvite inv = inviteRepo.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invite not found"));
        if (inv.getUsedAt() != null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invite already used");
        if (inv.getExpiresAt().isBefore(Instant.now())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invite expired");

        // Upsert user (create if missing)
        AppUser user = userRepo.findByEmail(inv.getEmail()).orElseGet(AppUser::new);
        user.setEmail(inv.getEmail());
        if (req.getPassword() != null && !req.getPassword().isBlank()) {
            user.setPasswordHash(encoder.encode(req.getPassword()));
        }
        user.setActive(true);
        // Store role as CSV (single role for now) so role parsing works correctly
        user.setRoles(inv.getRole());
        user.setOrgId(inv.getOrgId());
        if ("DRIVER".equalsIgnoreCase(inv.getRole())) {
            user.setDriverId(inv.getDriverId());
        }
        userRepo.save(user);

        inv.setUsedAt(Instant.now());
        inviteRepo.save(inv);

        // (Optional) Handle TOTP setup/verification here if you want to require MFA at activation.

        return ResponseEntity.ok(Map.of("status","activated"));
    }

    public record LoginReq(String email, String password, String totp) {}
    public record LoginRes(String accessToken, List<String> roles, Long driverId) {}

    @PostMapping("/login")
    public ResponseEntity<LoginRes> login(@RequestBody @Valid LoginReq req,
                                          HttpServletResponse res,
                                          HttpServletRequest httpReq) {
        var user = userRepo.findByEmail(req.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (!user.isActive() || !encoder.matches(req.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        String access = jwt.createAccessToken(new com.mediroute.service.security.AppUserView() {
            @Override public Long getId() { return user.getId(); }
            @Override public String getEmail() { return user.getEmail(); }
            @Override public List<String> getRoles() { return user.getRoleList(); }
            @Override public Long getDriverId() { return user.getDriverId(); }
            @Override public String getName() { return null; } // or user.getName() if/when you add it
        });

        var ttlDays = props.getSecurity().getRefreshTokenTtlDays();
        var rt = new RefreshToken();
        rt.setJti(UUID.randomUUID().toString());
        rt.setUserId(user.getId());
        rt.setExpiresAt(Instant.now().plus(Duration.ofDays(ttlDays)));
        rt.setUserAgent(httpReq.getHeader("User-Agent"));
        rt.setIp(httpReq.getRemoteAddr());
        rtRepo.save(rt);

        setRefreshCookie(res, rt.getJti(), (int) Duration.ofDays(ttlDays).toSeconds());
// after password check, BEFORE issuing tokens
        if (Boolean.TRUE.equals(user.isMfaEnabled())) {
            String totp = req.totp();
            if (totp == null || !validateCode(user.getMfaTotpSecret(), totp)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "mfa_required");
            }
        }
        return ResponseEntity.ok(new LoginRes(access, user.getRoles(), user.getDriverId()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginRes> refresh(
            @CookieValue(name = "refresh_token", required = false) String cookie,
            HttpServletResponse res,
            HttpServletRequest httpReq
    ) {
        if (cookie == null) cookie = findCookie(httpReq, props.getSecurity().getCookieName());
        if (cookie == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);

        var current = rtRepo.findByJti(cookie)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (current.isRevoked() || current.getExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        var user = userRepo.findById(current.getUserId()).orElseThrow();

        // rotate
        var ttlDays = props.getSecurity().getRefreshTokenTtlDays();
        var next = new RefreshToken();
        next.setJti(UUID.randomUUID().toString());
        next.setUserId(user.getId());
        next.setExpiresAt(Instant.now().plus(Duration.ofDays(ttlDays)));
        next.setUserAgent(httpReq.getHeader("User-Agent"));
        next.setIp(httpReq.getRemoteAddr());
        rtRepo.save(next);

        current.setRevoked(true);
        current.setRotatedTo(next.getJti());
        rtRepo.save(current);

        setRefreshCookie(res, next.getJti(), (int) Duration.ofDays(ttlDays).toSeconds());

        String access = jwt.createAccessToken(new com.mediroute.service.security.AppUserView() {
            @Override public Long getId() { return user.getId(); }
            @Override public String getEmail() { return user.getEmail(); }
            @Override public java.util.List<String> getRoles() { return user.getRoleList(); }
            @Override public Long getDriverId() { return user.getDriverId(); }
            @Override public String getName() { return null; }
        });
        return ResponseEntity.ok(new LoginRes(access, user.getRoles(), user.getDriverId()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "refresh_token", required = false) String cookie,
            HttpServletResponse res,
            HttpServletRequest req
    ) {
        if (cookie == null) cookie = findCookie(req, props.getSecurity().getCookieName());
        if (cookie != null) {
            rtRepo.findByJti(cookie).ifPresent(rt -> { rt.setRevoked(true); rtRepo.save(rt); });
            setRefreshCookie(res, "", 0); // clear
        }
        return ResponseEntity.noContent().build();
    }

    private String findCookie(HttpServletRequest req, String name) {
        if (req.getCookies() == null) return null;
        for (var c : req.getCookies()) if (name.equals(c.getName())) return c.getValue();
        return null;
    }

    private void setRefreshCookie(HttpServletResponse res, String value, int maxAgeSeconds) {
        boolean prod = !"dev".equalsIgnoreCase(System.getProperty("spring.profiles.active", "dev"));
        String sameSite = prod ? "Strict" : "Lax";
        boolean secure   = prod;

        var cookie = ResponseCookie.from(props.getSecurity().getCookieName(), value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/")
                .maxAge(maxAgeSeconds)
                .build();
        res.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    // --- MFA: begin TOTP setup (returns secret + otpauth URI) ---
    @PostMapping("/mfa/totp/setup")
    public ResponseEntity<?> totpSetup(@RequestHeader("Authorization") String authHeader) {
        var user = requireUserFromAuthHeader(authHeader); // helper shown below
        if (Boolean.TRUE.equals(user.getMfaEnabled()) && user.getMfaTotpSecret() != null) {
            return ResponseEntity.badRequest().body(Map.of("error","already_enabled"));
        }
        String secret = com.mediroute.service.security.TotpService.generateBase32Secret();
        user.setMfaTotpSecret(secret);
        userRepo.save(user);

        String issuer = props.getSecurity().getIssuer(); // e.g. https://mediroute.local
        String label = user.getEmail();
        String otpauth = com.mediroute.service.security.TotpService.buildOtpAuthUri(issuer, label, secret);

        return ResponseEntity.ok(Map.of(
                "secret", secret,
                "otpauthUri", otpauth
        ));
    }

    // --- MFA: verify a code and enable ---
    @PostMapping("/mfa/totp/verify")
    public ResponseEntity<?> totpVerify(@RequestHeader("Authorization") String authHeader,
                                        @RequestBody Map<String, String> body) {
        var user = requireUserFromAuthHeader(authHeader);
        String code = String.valueOf(body.getOrDefault("code","")).trim();
        String secret = user.getMfaTotpSecret();
        if (secret == null || secret.isBlank()) return ResponseEntity.badRequest().body(Map.of("error","no_secret"));

        boolean ok = validateCode(secret, code);
        if (!ok) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error","invalid_code"));

        user.setMfaEnabled(true);
        userRepo.save(user);
        return ResponseEntity.ok(Map.of("status","mfa_enabled"));
    }

    // --- helper: extract current user (since you have roles in JWT) ---
    private AppUser requireUserFromAuthHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        String token = authHeader.substring(7);
        var claims = jwt.parseAndValidate(token);
        String sub = String.valueOf(claims.get("sub"));
        try {
            Long userId = Long.valueOf(sub);
            return userRepo.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        } catch (NumberFormatException e) {
            // fallback if subject was email in some tokens
            return userRepo.findByEmail(sub).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        }
    }
}