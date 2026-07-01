package com.legacylock.backend.service;

import com.legacylock.backend.dto.request.CapsuleCreateRequest;
import com.legacylock.backend.dto.request.CapsuleUpdateRequest;
import com.legacylock.backend.dto.response.CapsuleResponse;
import com.legacylock.backend.entity.Capsule;
import com.legacylock.backend.entity.Users;
import com.legacylock.backend.enums.AuditAction;
import com.legacylock.backend.enums.CapsuleStatus;
import com.legacylock.backend.enums.Role;
import com.legacylock.backend.exceptions.LegacyLockException;
import com.legacylock.backend.repository.CapsuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CapsuleService {

    private final CapsuleRepository capsuleRepository;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;
    private final EncryptionService encryptionService;

    public CapsuleResponse createCapsule(CapsuleCreateRequest request) {

        Users owner = currentUserService.getCurrentUser();

        validateOwnerRole(owner);

        String normalizedTitle = request.getTitle().trim();

        boolean titleExists = capsuleRepository.existsByOwnerAndTitleAndStatusNot(
                owner,
                normalizedTitle,
                CapsuleStatus.DELETED
        );

        if (titleExists) {
            throw new LegacyLockException("Capsule with this title already exists");
        }

        String encryptedContent = encryptionService.encrypt(request.getContent());
        String contentHash = encryptionService.hashContent(request.getContent());

        Capsule capsule = Capsule.builder()
                .owner(owner)
                .title(normalizedTitle)
                .description(normalizeOptionalText(request.getDescription()))
                .content(null)
                .encryptedContent(encryptedContent)
                .contentHash(contentHash)
                .encryptionAlgorithm(encryptionService.getAlgorithm())
                .status(CapsuleStatus.DRAFT)
                .build();

        Capsule savedCapsule = capsuleRepository.save(capsule);

        auditLogService.log(
                owner,
                AuditAction.CAPSULE_CREATED,
                "CAPSULE",
                savedCapsule.getId(),
                "Capsule created: " + savedCapsule.getTitle()
        );

        return mapToResponse(savedCapsule);
    }

    @Transactional(readOnly = true)
    public List<CapsuleResponse> getMyCapsules() {

        Users owner = currentUserService.getCurrentUser();

        validateOwnerRole(owner);

        return capsuleRepository
                .findByOwnerAndStatusNotOrderByCreatedAtDesc(owner, CapsuleStatus.DELETED)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CapsuleResponse getCapsuleById(UUID capsuleId) {

        Users owner = currentUserService.getCurrentUser();

        validateOwnerRole(owner);

        Capsule capsule = getOwnedCapsuleOrThrow(capsuleId, owner);

        if (capsule.getStatus() == CapsuleStatus.DELETED) {
            throw new LegacyLockException("Capsule not found");
        }

        return mapToResponse(capsule);
    }

    public CapsuleResponse updateCapsule(
            UUID capsuleId,
            CapsuleUpdateRequest request
    ) {

        Users owner = currentUserService.getCurrentUser();

        validateOwnerRole(owner);

        Capsule capsule = getOwnedCapsuleOrThrow(capsuleId, owner);

        if (capsule.getStatus() == CapsuleStatus.DELETED) {
            throw new LegacyLockException("Capsule not found");
        }

        if (capsule.getStatus() == CapsuleStatus.RELEASED) {
            throw new LegacyLockException("Released capsule cannot be updated");
        }

        String normalizedTitle = request.getTitle().trim();

        boolean titleChanged = !capsule.getTitle().equals(normalizedTitle);

        if (titleChanged) {
            boolean titleExists = capsuleRepository.existsByOwnerAndTitleAndStatusNot(
                    owner,
                    normalizedTitle,
                    CapsuleStatus.DELETED
            );

            if (titleExists) {
                throw new LegacyLockException("Capsule with this title already exists");
            }
        }

        capsule.setTitle(normalizedTitle);
        capsule.setDescription(normalizeOptionalText(request.getDescription()));
        if (request.getContent() != null) {
            capsule.setContent(null);
            capsule.setEncryptedContent(encryptionService.encrypt(request.getContent()));
            capsule.setEncryptionAlgorithm(encryptionService.getAlgorithm());
            capsule.setContentHash(encryptionService.hashContent(request.getContent()));
        }

        Capsule updatedCapsule = capsuleRepository.save(capsule);

        auditLogService.log(
                owner,
                AuditAction.CAPSULE_UPDATED,
                "CAPSULE",
                updatedCapsule.getId(),
                "Capsule updated: " + updatedCapsule.getTitle()
        );

        return mapToResponse(updatedCapsule);
    }

    public CapsuleResponse activateCapsule(UUID capsuleId) {

        Users owner = currentUserService.getCurrentUser();

        validateOwnerRole(owner);

        Capsule capsule = getOwnedCapsuleOrThrow(capsuleId, owner);

        if (capsule.getStatus() == CapsuleStatus.DELETED) {
            throw new LegacyLockException("Capsule not found");
        }

        if (capsule.getStatus() == CapsuleStatus.RELEASED) {
            throw new LegacyLockException("Released capsule cannot be activated again");
        }

        capsule.setStatus(CapsuleStatus.ACTIVE);

        Capsule activatedCapsule = capsuleRepository.save(capsule);

        auditLogService.log(
                owner,
                AuditAction.CAPSULE_ACTIVATED,
                "CAPSULE",
                activatedCapsule.getId(),
                "Capsule activated: " + activatedCapsule.getTitle()
        );

        return mapToResponse(activatedCapsule);
    }

    public void deleteCapsule(UUID capsuleId) {

        Users owner = currentUserService.getCurrentUser();

        validateOwnerRole(owner);

        Capsule capsule = getOwnedCapsuleOrThrow(capsuleId, owner);

        if (capsule.getStatus() == CapsuleStatus.DELETED) {
            throw new LegacyLockException("Capsule already deleted");
        }

        if (capsule.getStatus() == CapsuleStatus.RELEASED) {
            throw new LegacyLockException("Released capsule cannot be deleted");
        }

        capsule.setStatus(CapsuleStatus.DELETED);
        capsule.setDeletedAt(LocalDateTime.now());

        capsuleRepository.save(capsule);

        auditLogService.log(
                owner,
                AuditAction.CAPSULE_DELETED,
                "CAPSULE",
                capsule.getId(),
                "Capsule deleted: " + capsule.getTitle()
        );
    }

    private Capsule getOwnedCapsuleOrThrow(UUID capsuleId, Users owner) {
        return capsuleRepository.findByIdAndOwner(capsuleId, owner)
                .orElseThrow(() -> new LegacyLockException("Capsule not found"));
    }

    private void validateOwnerRole(Users user) {
        if (!user.getRoles().contains(Role.OWNER)) {
            throw new LegacyLockException("Only owners can manage capsules");
        }
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return value.trim();
    }

    private CapsuleResponse mapToResponse(Capsule capsule) {
        return CapsuleResponse.builder()
                .id(capsule.getId())
                .title(capsule.getTitle())
                .description(capsule.getDescription())
                .content(encryptionService.decrypt(capsule.getEncryptedContent()))
                .encryptionAlgorithm(capsule.getEncryptionAlgorithm())
                .contentHash(capsule.getContentHash())
                .status(capsule.getStatus())
                .createdAt(capsule.getCreatedAt())
                .updatedAt(capsule.getUpdatedAt())
                .build();
    }
}
