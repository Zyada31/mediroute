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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.springframework.web.server.ResponseStatusException;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.mediroute.service.security.TotpService.validateCode;

/**
 * Authentication & User Activation/Invite Controller
 * - Login/refresh/logout with stateless JWT
 * - Invite creation and activation flows (includes MFA checks)
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Authentication, invite, and activation APIs")
@Slf4j
public class AuthAndInviteController {

	private final UserRepo userRepo;
	private final RefreshTokenRepo rtRepo;
	private final PasswordEncoder encoder;
	private final JwtService jwt;
	private final AppProps props;
	private final UserInviteRepo inviteRepo;

	public AuthAndInviteController(UserRepo userRepo, RefreshTokenRepo rtRepo,
							PasswordEncoder encoder, JwtService jwt, AppProps props,
							UserInviteRepo inviteRepo) {
		this.userRepo = userRepo; this.rtRepo = rtRepo; this.encoder = encoder; this.jwt = jwt; this.props = props;
		this.inviteRepo = inviteRepo;
	}

	@PostMapping("/activate")
	@Operation(summary = "Activate invited user", description = "Activate a user account using invite token and optional password")
    @RateLimiter(name = "auth-rl")
    public ResponseEntity<?> activate(@RequestBody @Valid ActivateReq req) {
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

		AppUser user = userRepo.findByEmail(inv.getEmail()).orElseGet(AppUser::new);
		user.setEmail(inv.getEmail());
		if (req.getPassword() != null && !req.getPassword().isBlank()) {
			user.setPasswordHash(encoder.encode(req.getPassword()));
		}
		user.setActive(true);
		user.setRoles(inv.getRole());
		user.setOrgId(inv.getOrgId());
		if ("DRIVER".equalsIgnoreCase(inv.getRole())) {
			user.setDriverId(inv.getDriverId());
		}
		userRepo.save(user);

		inv.setUsedAt(Instant.now());
		inviteRepo.save(inv);

		log.info("User activation succeeded for {}", user.getEmail());
		return ResponseEntity.ok(Map.of("status","activated"));
	}

    public record LoginReq(String email, String password, String totp) {}
    public record LoginRes(String accessToken, List<String> roles, Long driverId, String refreshToken) {}

	@PostMapping("/login")
	@Operation(summary = "Login", description = "Authenticate using email/password (+ optional TOTP) and receive access token + refresh cookie")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Login successful"),
			@ApiResponse(responseCode = "401", description = "Unauthorized")
	})
    @RateLimiter(name = "auth-rl")
    public ResponseEntity<LoginRes> login(@RequestBody @Valid LoginReq req,
										  HttpServletResponse res,
										  HttpServletRequest httpReq) {
		var user = userRepo.findByEmail(req.email())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
		if (!user.isActive() || !encoder.matches(req.password(), user.getPasswordHash())) {
			log.warn("Login failed for {}", req.email());
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
		}

		String access = jwt.createAccessToken(new com.mediroute.service.security.AppUserView() {
			@Override public Long getId() { return user.getId(); }
			@Override public String getEmail() { return user.getEmail(); }
			@Override public List<String> getRoles() { return user.getRoleList(); }
			@Override public Long getDriverId() { return user.getDriverId(); }
			@Override public String getName() { return null; }
		});

		var ttlDays = props.getSecurity().getRefreshTokenTtlDays();
		var rt = new RefreshToken();
		rt.setJti(UUID.randomUUID().toString());
		rt.setUserId(user.getId());
		rt.setExpiresAt(Instant.now().plus(Duration.ofDays(ttlDays)));
		rt.setUserAgent(httpReq.getHeader("User-Agent"));
		rt.setIp(httpReq.getRemoteAddr());
		rtRepo.save(rt);

        setRefreshCookie(httpReq, res, rt.getJti(), (int) Duration.ofDays(ttlDays).toSeconds());
		if (Boolean.TRUE.equals(user.isMfaEnabled())) {
			String totp = req.totp();
			if (totp == null || !validateCode(user.getMfaTotpSecret(), totp)) {
				log.warn("MFA required/failed for {}", req.email());
				throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "mfa_required");
			}
		}
        boolean dev = "dev".equalsIgnoreCase(System.getenv().getOrDefault("SPRING_PROFILES_ACTIVE", "dev"));
        log.info("Login succeeded for {}", req.email());
        return ResponseEntity.ok(new LoginRes(access, user.getRoles(), user.getDriverId(), dev ? rt.getJti() : null));
	}

	@PostMapping("/refresh")
	@Operation(summary = "Refresh access token", description = "Rotate refresh token cookie and issue new access token")
    @RateLimiter(name = "auth-rl")
    public ResponseEntity<LoginRes> refresh(
			@CookieValue(name = "refresh_token", required = false) String cookie,
			HttpServletResponse res,
			HttpServletRequest httpReq
	) {
        if (cookie == null) cookie = findCookie(httpReq, props.getSecurity().getCookieName());
        if (cookie == null) cookie = httpReq.getHeader("X-Refresh-Token");
		if (cookie == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);

		var current = rtRepo.findByJti(cookie)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
		if (current.isRevoked() || current.getExpiresAt().isBefore(Instant.now())) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
		}

		var user = userRepo.findById(current.getUserId()).orElseThrow();

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

        setRefreshCookie(httpReq, res, next.getJti(), (int) Duration.ofDays(ttlDays).toSeconds());

		String access = jwt.createAccessToken(new com.mediroute.service.security.AppUserView() {
			@Override public Long getId() { return user.getId(); }
			@Override public String getEmail() { return user.getEmail(); }
			@Override public java.util.List<String> getRoles() { return user.getRoleList(); }
			@Override public Long getDriverId() { return user.getDriverId(); }
			@Override public String getName() { return null; }
		});
        boolean dev = "dev".equalsIgnoreCase(System.getenv().getOrDefault("SPRING_PROFILES_ACTIVE", "dev"));
        log.info("Refresh token rotated for user {}", user.getEmail());
        return ResponseEntity.ok(new LoginRes(access, user.getRoles(), user.getDriverId(), dev ? next.getJti() : null));
	}

	@PostMapping("/logout")
	@Operation(summary = "Logout", description = "Revoke refresh token and clear cookie")
    @RateLimiter(name = "auth-rl")
    public ResponseEntity<Void> logout(
			@CookieValue(name = "refresh_token", required = false) String cookie,
			HttpServletResponse res,
			HttpServletRequest req
	) {
		if (cookie == null) cookie = findCookie(req, props.getSecurity().getCookieName());
		if (cookie != null) {
			rtRepo.findByJti(cookie).ifPresent(rt -> { rt.setRevoked(true); rtRepo.save(rt); });
            setRefreshCookie(req, res, "", 0);
		}
		log.info("User logged out (refresh revoked if present)");
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/mfa/totp/setup")
	@Operation(summary = "Begin TOTP setup", description = "Generate a TOTP secret and return otpauth URI for the current user")
    @RateLimiter(name = "auth-rl")
    public ResponseEntity<?> totpSetup(@RequestHeader("Authorization") String authHeader) {
		var user = requireUserFromAuthHeader(authHeader);
		if (Boolean.TRUE.equals(user.getMfaEnabled()) && user.getMfaTotpSecret() != null) {
			return ResponseEntity.badRequest().body(Map.of("error","already_enabled"));
		}
		String secret = com.mediroute.service.security.TotpService.generateBase32Secret();
		user.setMfaTotpSecret(secret);
		userRepo.save(user);

		String issuer = props.getSecurity().getIssuer();
		String label = user.getEmail();
		String otpauth = com.mediroute.service.security.TotpService.buildOtpAuthUri(issuer, label, secret);

		return ResponseEntity.ok(Map.of(
				"secret", secret,
				"otpauthUri", otpauth
		));
	}

	@PostMapping("/mfa/totp/verify")
	@Operation(summary = "Verify TOTP and enable", description = "Verify a code against the stored secret and enable MFA")
    @RateLimiter(name = "auth-rl")
    public ResponseEntity<?> totpVerify(@RequestHeader("Authorization") String authHeader,
										  @RequestBody Map<String, String> body) {
		var user = requireUserFromAuthHeader(authHeader);
		String code = String.valueOf(body.getOrDefault("code",""))
				.trim();
		String secret = user.getMfaTotpSecret();
		if (secret == null || secret.isBlank()) return ResponseEntity.badRequest().body(Map.of("error","no_secret"));

		boolean ok = validateCode(secret, code);
		if (!ok) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error","invalid_code"));

		user.setMfaEnabled(true);
		userRepo.save(user);
		log.info("MFA enabled for user {}", user.getEmail());
		return ResponseEntity.ok(Map.of("status","mfa_enabled"));
	}

	@PostMapping("/invite")
	@Operation(summary = "Create user invite", description = "Create an invite link for a new or inactive user in an org")
	@PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    @RateLimiter(name = "auth-rl")
    public ResponseEntity<com.mediroute.dto.InviteCreateRes> invite(@RequestBody com.mediroute.dto.InviteCreateReq req) {
		userRepo.findByEmail(req.getEmail()).ifPresent(u -> {
			if (u.isActive()) throw new IllegalStateException("User already active");
		});

		String rawToken = java.util.UUID.randomUUID().toString().replace("-", "");
		String tokenHash = org.springframework.util.DigestUtils.md5DigestAsHex(rawToken.getBytes(java.nio.charset.StandardCharsets.UTF_8));

		UserInvite inv = UserInvite.builder()
				.email(req.getEmail())
				.role(req.getRole())
				.orgId(req.getOrgId())
				.driverId(req.getDriverId())
				.tokenHash(tokenHash)
				.expiresAt(java.time.Instant.now().plus(java.time.Duration.ofDays(7)))
				.createdAt(java.time.Instant.now())
				.build();
		inviteRepo.save(inv);

		String token = rawToken + "." + tokenHash.substring(0, 8);
		String base = props.getSecurity().getIssuer();
		String url = base + "/activate?token=" + java.net.URLEncoder.encode(token, java.nio.charset.StandardCharsets.UTF_8);

		log.info("Invite created for {} role={} orgId={}", req.getEmail(), req.getRole(), req.getOrgId());
		return ResponseEntity.ok(new com.mediroute.dto.InviteCreateRes(url));
	}

	private String findCookie(HttpServletRequest req, String name) {
		if (req.getCookies() == null) return null;
		for (var c : req.getCookies()) if (name.equals(c.getName())) return c.getValue();
		return null;
	}

    private void setRefreshCookie(HttpServletRequest req, HttpServletResponse res, String value, int maxAgeSeconds) {
        String active = System.getenv().getOrDefault("SPRING_PROFILES_ACTIVE", "dev");
        boolean dev = "dev".equalsIgnoreCase(active);
        boolean requestSecure = req.isSecure() || "https".equalsIgnoreCase(req.getHeader("X-Forwarded-Proto"));
        boolean secure = dev ? false : requestSecure;
        String sameSite = dev ? "Lax" : (secure ? "None" : "Strict");

		var cookie = ResponseCookie.from(props.getSecurity().getCookieName(), value)
				.httpOnly(true)
				.secure(secure)
				.sameSite(sameSite)
				.path("/")
				.maxAge(maxAgeSeconds)
				.build();
        res.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
	}

	private AppUser requireUserFromAuthHeader(String authHeader) {
		if (authHeader == null || !authHeader.startsWith("Bearer ")) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
		String token = authHeader.substring(7);
		var claims = jwt.parseAndValidate(token);
		String sub = String.valueOf(claims.get("sub"));
		try {
			Long userId = Long.valueOf(sub);
			return userRepo.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
		} catch (NumberFormatException e) {
			return userRepo.findByEmail(sub).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
		}
	}
}
