package com.legacylock.backend.service;

import com.legacylock.backend.exceptions.LegacyLockException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class EncryptionService {

    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;

    @Value("${app.encryption.secret}")
    private String encryptionSecret;

    @Value("${app.encryption.algorithm}")
    private String encryptionAlgorithm;

    private final SecureRandom secureRandom = new SecureRandom();

    public String encrypt(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            return null;
        }

        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(encryptionAlgorithm);

            SecretKeySpec keySpec = new SecretKeySpec(
                    getSecretKeyBytes(),
                    "AES"
            );

            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(
                    TAG_LENGTH_BITS,
                    iv
            );

            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmParameterSpec);

            byte[] encryptedBytes = cipher.doFinal(
                    plainText.getBytes(StandardCharsets.UTF_8)
            );

            String encodedIv = Base64.getEncoder().encodeToString(iv);
            String encodedCipherText = Base64.getEncoder().encodeToString(encryptedBytes);

            return encodedIv + ":" + encodedCipherText;

        } catch (Exception e) {
            throw new LegacyLockException("Could not encrypt capsule content: " + e.getMessage());
        }
    }

    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isBlank()) {
            return null;
        }

        try {
            String[] parts = encryptedText.split(":");

            if (parts.length != 2) {
                throw new LegacyLockException("Invalid encrypted content format");
            }

            byte[] iv = Base64.getDecoder().decode(parts[0]);
            byte[] encryptedBytes = Base64.getDecoder().decode(parts[1]);

            Cipher cipher = Cipher.getInstance(encryptionAlgorithm);

            SecretKeySpec keySpec = new SecretKeySpec(
                    getSecretKeyBytes(),
                    "AES"
            );

            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(
                    TAG_LENGTH_BITS,
                    iv
            );

            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmParameterSpec);

            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

            return new String(decryptedBytes, StandardCharsets.UTF_8);

        } catch (LegacyLockException e) {
            throw e;
        } catch (Exception e) {
            throw new LegacyLockException("Could not decrypt capsule content: " + e.getMessage());
        }
    }

    public String hashContent(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            return null;
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new LegacyLockException("Could not hash capsule content");
        }
    }

    public String getAlgorithm() {
        return encryptionAlgorithm;
    }

    private byte[] getSecretKeyBytes() {
        try {
            if (encryptionSecret == null || encryptionSecret.isBlank()) {
                throw new LegacyLockException("Encryption secret is missing");
            }

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(encryptionSecret.getBytes(StandardCharsets.UTF_8));

        } catch (LegacyLockException e) {
            throw e;
        } catch (Exception e) {
            throw new LegacyLockException("Could not generate encryption key");
        }
    }
}
