package com.legacylock.backend.repository;

import com.legacylock.backend.entity.RefreshToken;
import com.legacylock.backend.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findByUserAndRevokedFalse(Users user);

    void deleteByUser(Users user);
}
