package com.legacylock.backend.repository;

import com.legacylock.backend.entity.PasswordResetToken;
import com.legacylock.backend.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByTokenHashAndUsedFalse(String tokenHash);

    void deleteByUser(Users user);
}
