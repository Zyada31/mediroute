// src/main/java/com/mediroute/service/security/JwtService.java
package com.mediroute.service.security;

import com.mediroute.config.AppProps;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.security.oauth2.jwt.*;

import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.source.*;
import com.nimbusds.jose.proc.SecurityContext;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * JWT service for issuing and validating access tokens.
 */
@Service
public class JwtService {
    private final String issuer;
    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final AppProps props;

    public JwtService(
            AppProps props,
            @Value("${app.security.issuer:https://mediroute.local}") String issuer,
            @Value("${jwt.private-key-pem}") String privatePem,
            @Value("${jwt.public-key-pem}") String publicPem
    ) {
        RSAPrivateKey priv = PemUtils.readPrivateKey(privatePem);
        RSAPublicKey pub = PemUtils.readPublicKey(publicPem);

        RSAKey rsa = new RSAKey.Builder(pub)
                .privateKey(priv)
                .keyID(UUID.randomUUID().toString())
                .build();

        JWKSource<SecurityContext> jwks = new ImmutableJWKSet<>(new JWKSet(rsa));

        this.encoder = new NimbusJwtEncoder(jwks);
        this.decoder = NimbusJwtDecoder.withPublicKey(pub).build();
        this.issuer = issuer;
        this.props = props;
    }

    /** Low-level helper used by the convenience creators. */
    public String issueAccessToken(String subject, Map<String, Object> claims, Duration ttl) {
        Instant now = Instant.now();
        JwtClaimsSet.Builder b = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                .subject(subject);

        if (claims != null) b.claims(c -> c.putAll(claims));

        JwtClaimsSet set = b.build();
        return encoder.encode(JwtEncoderParameters.from(set)).getTokenValue();
    }

    /** Parse & validate; throws if invalid/expired. */
    public Map<String, Object> parseAndValidate(String token) {
        Jwt jwt = decoder.decode(token);
        return jwt.getClaims();
    }

    // ------------------------------------------------------------
    // Convenience creators
    // ------------------------------------------------------------

    /**
     * Create an access token from your user entity.
     * Adjust the method parameter type if your entity isn't named AppUser.
     */
    public String createAccessToken(AppUserLike user) {
        // TTL from config (minutes)
        long ttlMin = props.getSecurity().getAccessTokenTtlMin();
        Duration ttl = Duration.ofMinutes(ttlMin <= 0 ? 10 : ttlMin);

        Map<String, Object> claims = new HashMap<>();
        claims.put("email", user.getEmail());
        claims.put("roles", user.getRoles());             // List<String>
        if (user.getDriverId() != null) claims.put("driverId", user.getDriverId());
        if (user.getName() != null) claims.put("name", user.getName());
        claims.put("type", "access");
        claims.put("jti", UUID.randomUUID().toString());

        // subject = user id
        return issueAccessToken(String.valueOf(user.getId()), claims, ttl);
    }

    /**
     * Create an access token without tying to a specific entity class.
     */
    public String createAccessToken(AppUserView user) {
        long ttlMin = props.getSecurity().getAccessTokenTtlMin();
        var ttl = Duration.ofMinutes(ttlMin <= 0 ? 10 : ttlMin);

        Map<String, Object> claims = new HashMap<>();
        claims.put("email", user.getEmail());
        claims.put("roles", user.getRoles() == null ? List.of() : user.getRoles());
        if (user.getDriverId() != null) claims.put("driverId", user.getDriverId());
        if (user.getName() != null) claims.put("name", user.getName());
        claims.put("type", "access");
        claims.put("jti", UUID.randomUUID().toString());

        return issueAccessToken(String.valueOf(user.getId()), claims, ttl);
    }

    // ----------------------------------------------------------------
    // Small adapter interface so we don't hard-couple JwtService
    // to a specific entity class name. Your AppUser can implement this.
    // ----------------------------------------------------------------
    public interface AppUserLike {
        Long getId();
        String getEmail();
        List<String> getRoles();
        Long getDriverId();           // can return null
        String getName();             // can return null
    }
}