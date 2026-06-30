package com.legacylock.backend.service;

import com.legacylock.backend.entity.RefreshToken;
import com.legacylock.backend.entity.Users;
import com.legacylock.backend.exceptions.LegacyLockException;
import com.legacylock.backend.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Transactional
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpirationMs;

    private final SecureRandom secureRandom = new SecureRandom();

    public String createRefreshToken(Users user) {
        String rawToken = generateRawToken();
        String tokenHash = hashToken(rawToken);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plusNanos(refreshExpirationMs * 1_000_000))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }

    public Users getUserFromRefreshToken(String rawToken) {
        RefreshToken refreshToken = getValidRefreshToken(rawToken);
        return refreshToken.getUser();
    }

    public String rotateRefreshToken(String oldRawToken) {
        RefreshToken oldToken = getValidRefreshToken(oldRawToken);

        String newRawToken = generateRawToken();
        String newTokenHash = hashToken(newRawToken);

        RefreshToken newToken = RefreshToken.builder()
                .user(oldToken.getUser())
                .tokenHash(newTokenHash)
                .expiresAt(LocalDateTime.now().plusNanos(refreshExpirationMs * 1_000_000))
                .revoked(false)
                .build();

        RefreshToken savedNewToken = refreshTokenRepository.save(newToken);

        oldToken.setRevoked(true);
        oldToken.setRevokedAt(LocalDateTime.now());
        oldToken.setReplacedByToken(savedNewToken);

        refreshTokenRepository.save(oldToken);

        return newRawToken;
    }

    public void revokeRefreshToken(String rawToken) {
        RefreshToken refreshToken = getValidRefreshToken(rawToken);

        refreshToken.setRevoked(true);
        refreshToken.setRevokedAt(LocalDateTime.now());

        refreshTokenRepository.save(refreshToken);
    }

    private RefreshToken getValidRefreshToken(String rawToken) {
        String tokenHash = hashToken(rawToken);

        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new LegacyLockException("Invalid refresh token"));

        if (refreshToken.isRevoked()) {
            throw new LegacyLockException("Refresh token has been revoked");
        }

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new LegacyLockException("Refresh token has expired");
        }

        return refreshToken;
    }

    private String generateRawToken() {
        byte[] randomBytes = new byte[64];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new LegacyLockException("Could not hash refresh token");
        }
    }
}
