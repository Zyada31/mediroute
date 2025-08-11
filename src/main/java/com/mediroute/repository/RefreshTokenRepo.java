package com.mediroute.repository;

import com.mediroute.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepo extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByJti(String jti);
    List<RefreshToken> findByUserIdAndRevokedFalse(Long userId);
    long deleteByExpiresAtBefore(Instant cutoff); // for cleanup job
}