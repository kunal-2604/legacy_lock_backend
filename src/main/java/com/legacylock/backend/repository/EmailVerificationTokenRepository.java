package com.legacylock.backend.repository;

import com.legacylock.backend.entity.EmailVerificationToken;
import com.legacylock.backend.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {

    Optional<EmailVerificationToken> findByTokenHashAndUsedFalse(String tokenHash);

    void deleteByUser(Users user);
}
