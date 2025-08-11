package com.mediroute.controller;

import com.mediroute.config.AppProps;
import com.mediroute.entity.RefreshToken;
import com.mediroute.repository.RefreshTokenRepo;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth") // unified prefix
public class AuthController {
    private final UserRepo userRepo;
    private final RefreshTokenRepo rtRepo;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final AppProps props;

    public AuthController(UserRepo userRepo, RefreshTokenRepo rtRepo,
                          PasswordEncoder encoder, JwtService jwt, AppProps props) {
        this.userRepo = userRepo; this.rtRepo = rtRepo; this.encoder = encoder; this.jwt = jwt; this.props = props;
    }

    public record LoginReq(String email, String password) {}
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

        String access = jwt.createAccessToken((JwtService.AppUserLike) user);

        var ttlDays = props.getSecurity().getRefreshTokenTtlDays();
        var rt = new RefreshToken();
        rt.setJti(UUID.randomUUID().toString());
        rt.setUserId(user.getId());
        rt.setExpiresAt(Instant.now().plus(Duration.ofDays(ttlDays)));
        rt.setUserAgent(httpReq.getHeader("User-Agent"));
        rt.setIp(httpReq.getRemoteAddr());
        rtRepo.save(rt);

        setRefreshCookie(res, rt.getJti(), (int) Duration.ofDays(ttlDays).toSeconds());

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

        String access = jwt.createAccessToken((JwtService.AppUserLike) user);
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
}