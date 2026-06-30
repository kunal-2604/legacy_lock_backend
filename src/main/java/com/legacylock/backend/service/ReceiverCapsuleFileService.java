package com.legacylock.backend.service;

import com.legacylock.backend.dto.response.CapsuleFileResponse;
import com.legacylock.backend.dto.response.ReceiverCapsuleFileDownloadResponse;
import com.legacylock.backend.entity.AccessGrant;
import com.legacylock.backend.entity.Capsule;
import com.legacylock.backend.entity.CapsuleFile;
import com.legacylock.backend.entity.Users;
import com.legacylock.backend.enums.AccessGrantStatus;
import com.legacylock.backend.enums.AuditAction;
import com.legacylock.backend.enums.CapsuleFileStatus;
import com.legacylock.backend.enums.CapsuleStatus;
import com.legacylock.backend.exceptions.LegacyLockException;
import com.legacylock.backend.repository.AccessGrantRepository;
import com.legacylock.backend.repository.CapsuleFileRepository;
import com.legacylock.backend.repository.CapsuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ReceiverCapsuleFileService {

    private final CapsuleRepository capsuleRepository;
    private final CapsuleFileRepository capsuleFileRepository;
    private final AccessGrantRepository accessGrantRepository;
    private final CurrentUserService currentUserService;
    private final S3StorageService s3StorageService;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<CapsuleFileResponse> getReleasedCapsuleFiles(UUID capsuleId) {

        Users currentUser = currentUserService.getCurrentUser();

        Capsule capsule = getAccessibleReleasedCapsule(capsuleId, currentUser);

        return capsuleFileRepository
                .findByCapsuleAndStatusOrderByUploadedAtDesc(
                        capsule,
                        CapsuleFileStatus.ACTIVE
                )
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public ReceiverCapsuleFileDownloadResponse downloadReleasedCapsuleFile(
            UUID capsuleId,
            UUID fileId
    ) {
        Users currentUser = currentUserService.getCurrentUser();

        Capsule capsule = getAccessibleReleasedCapsule(capsuleId, currentUser);

        CapsuleFile capsuleFile = capsuleFileRepository
                .findByIdAndCapsuleAndStatus(
                        fileId,
                        capsule,
                        CapsuleFileStatus.ACTIVE
                )
                .orElseThrow(() -> new LegacyLockException("File not found"));

        byte[] fileBytes = s3StorageService.downloadFile(
                capsuleFile.getStoredFileKey()
        );

        auditLogService.log(
                currentUser,
                AuditAction.CAPSULE_FILE_DOWNLOADED_BY_RECEIVER,
                "CAPSULE_FILE",
                capsuleFile.getId(),
                "Receiver downloaded file from released capsule " + capsule.getId()
        );

        return ReceiverCapsuleFileDownloadResponse.builder()
                .originalFileName(capsuleFile.getOriginalFileName())
                .contentType(capsuleFile.getContentType())
                .fileBytes(fileBytes)
                .build();
    }

    private Capsule getAccessibleReleasedCapsule(UUID capsuleId, Users currentUser) {

        Capsule capsule = capsuleRepository.findById(capsuleId)
                .orElseThrow(() -> new LegacyLockException("Capsule not found"));

        if (capsule.getStatus() != CapsuleStatus.RELEASED) {
            throw new LegacyLockException("Capsule is not released yet");
        }

        boolean hasActiveGrant = accessGrantRepository
                .existsByCapsuleAndReceiver_EmailAndStatus(
                        capsule,
                        currentUser.getEmail(),
                        AccessGrantStatus.ACTIVE
                );

        if (!hasActiveGrant) {
            throw new LegacyLockException("You do not have access to this capsule");
        }

        return capsule;
    }

    private CapsuleFileResponse mapToResponse(CapsuleFile capsuleFile) {
        return CapsuleFileResponse.builder()
                .id(capsuleFile.getId())
                .capsuleId(capsuleFile.getCapsule().getId())
                .originalFileName(capsuleFile.getOriginalFileName())
                .storedFileKey(null)
                .contentType(capsuleFile.getContentType())
                .fileSize(capsuleFile.getFileSize())
                .checksum(capsuleFile.getChecksum())
                .status(capsuleFile.getStatus())
                .uploadedAt(capsuleFile.getUploadedAt())
                .build();
    }
}
