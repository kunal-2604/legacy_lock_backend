package com.legacylock.backend.service;

import com.legacylock.backend.dto.response.CapsuleFileResponse;
import com.legacylock.backend.entity.Capsule;
import com.legacylock.backend.entity.CapsuleFile;
import com.legacylock.backend.entity.Users;
import com.legacylock.backend.enums.AuditAction;
import com.legacylock.backend.enums.CapsuleFileStatus;
import com.legacylock.backend.enums.CapsuleStatus;
import com.legacylock.backend.exceptions.LegacyLockException;
import com.legacylock.backend.repository.CapsuleFileRepository;
import com.legacylock.backend.repository.CapsuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CapsuleFileService {

    private final CapsuleRepository capsuleRepository;
    private final CapsuleFileRepository capsuleFileRepository;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;
    private final S3StorageService s3StorageService;

    public CapsuleFileResponse uploadFile(UUID capsuleId, MultipartFile file) {

        Users currentUser = currentUserService.getCurrentUser();
        Capsule capsule = getOwnedCapsule(capsuleId, currentUser);

        validateCapsuleAllowsFileChanges(capsule);

        if (file == null || file.isEmpty()) {
            throw new LegacyLockException("File is required");
        }

        String originalFileName = cleanFileName(file.getOriginalFilename());

        String storedFileKey = buildStoredFileKey(
                currentUser.getId(),
                capsule.getId(),
                originalFileName
        );

        String checksum = calculateChecksum(file);

        // Step 1: upload real file to S3
        s3StorageService.uploadFile(file, storedFileKey);

        // Step 2: save metadata in PostgreSQL
        CapsuleFile capsuleFile = CapsuleFile.builder()
                .capsule(capsule)
                .originalFileName(originalFileName)
                .storedFileKey(storedFileKey)
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .checksum(checksum)
                .status(CapsuleFileStatus.ACTIVE)
                .build();

        CapsuleFile savedFile = capsuleFileRepository.save(capsuleFile);

        auditLogService.log(
                currentUser,
                AuditAction.CAPSULE_FILE_UPLOADED,
                "CAPSULE_FILE",
                savedFile.getId(),
                "File uploaded to S3 for capsule " + capsule.getId()
        );

        return mapToResponse(savedFile);
    }

    @Transactional(readOnly = true)
    public List<CapsuleFileResponse> getFiles(UUID capsuleId) {

        Users currentUser = currentUserService.getCurrentUser();
        Capsule capsule = getOwnedCapsule(capsuleId, currentUser);

        return capsuleFileRepository
                .findByCapsuleAndStatusOrderByUploadedAtDesc(
                        capsule,
                        CapsuleFileStatus.ACTIVE
                )
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public void deleteFile(UUID capsuleId, UUID fileId) {

        Users currentUser = currentUserService.getCurrentUser();
        Capsule capsule = getOwnedCapsule(capsuleId, currentUser);

        validateCapsuleAllowsFileChanges(capsule);

        CapsuleFile capsuleFile = capsuleFileRepository
                .findByIdAndCapsuleAndStatus(
                        fileId,
                        capsule,
                        CapsuleFileStatus.ACTIVE
                )
                .orElseThrow(() -> new LegacyLockException("File not found"));

        // Step 1: delete real file from S3
        s3StorageService.deleteFile(capsuleFile.getStoredFileKey());

        // Step 2: soft-delete metadata
        capsuleFile.setStatus(CapsuleFileStatus.DELETED);
        capsuleFile.setDeletedAt(LocalDateTime.now());

        capsuleFileRepository.save(capsuleFile);

        auditLogService.log(
                currentUser,
                AuditAction.CAPSULE_FILE_DELETED,
                "CAPSULE_FILE",
                capsuleFile.getId(),
                "File deleted from S3 for capsule " + capsule.getId()
        );
    }

    private Capsule getOwnedCapsule(UUID capsuleId, Users currentUser) {
        Capsule capsule = capsuleRepository.findById(capsuleId)
                .orElseThrow(() -> new LegacyLockException("Capsule not found"));

        if (!capsule.getOwner().getId().equals(currentUser.getId())) {
            throw new LegacyLockException("You are not allowed to access this capsule");
        }

        return capsule;
    }

    private void validateCapsuleAllowsFileChanges(Capsule capsule) {
        if (capsule.getStatus() == CapsuleStatus.RELEASED) {
            throw new LegacyLockException("Cannot modify files of a released capsule");
        }

        if (capsule.getStatus() == CapsuleStatus.DELETED) {
            throw new LegacyLockException("Cannot modify files of a deleted capsule");
        }
    }

    private String buildStoredFileKey(
            UUID ownerId,
            UUID capsuleId,
            String originalFileName
    ) {
        return "owners/"
                + ownerId
                + "/capsules/"
                + capsuleId
                + "/files/"
                + UUID.randomUUID()
                + "-"
                + originalFileName;
    }

    private String cleanFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "uploaded-file";
        }

        return fileName
                .replace("\\", "_")
                .replace("/", "_")
                .replace("..", "_");
    }

    private String calculateChecksum(MultipartFile file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            try (InputStream inputStream = file.getInputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }

            return Base64.getEncoder().encodeToString(digest.digest());

        } catch (Exception e) {
            throw new LegacyLockException("Could not calculate file checksum");
        }
    }

    private CapsuleFileResponse mapToResponse(CapsuleFile capsuleFile) {
        return CapsuleFileResponse.builder()
                .id(capsuleFile.getId())
                .capsuleId(capsuleFile.getCapsule().getId())
                .originalFileName(capsuleFile.getOriginalFileName())
                .storedFileKey(capsuleFile.getStoredFileKey())
                .contentType(capsuleFile.getContentType())
                .fileSize(capsuleFile.getFileSize())
                .checksum(capsuleFile.getChecksum())
                .status(capsuleFile.getStatus())
                .uploadedAt(capsuleFile.getUploadedAt())
                .build();
    }
}
