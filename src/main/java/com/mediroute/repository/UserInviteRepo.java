package com.mediroute.repository;

import com.mediroute.entity.UserInvite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserInviteRepo extends JpaRepository<UserInvite, Long> {
    Optional<UserInvite> findByTokenHash(String tokenHash);
}