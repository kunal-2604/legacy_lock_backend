package com.legacylock.backend.service;

import com.legacylock.backend.dto.response.ReceiverCapsuleDetailResponse;
import com.legacylock.backend.dto.response.ReceiverCapsuleResponse;
import com.legacylock.backend.entity.AccessGrant;
import com.legacylock.backend.entity.Capsule;
import com.legacylock.backend.entity.Receiver;
import com.legacylock.backend.entity.Users;
import com.legacylock.backend.enums.*;
import com.legacylock.backend.exceptions.LegacyLockException;
import com.legacylock.backend.repository.AccessGrantRepository;
import com.legacylock.backend.repository.ReceiverAcknowledgementRepository;
import com.legacylock.backend.repository.ReceiverRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReceiverAccessService {

    private final CurrentUserService currentUserService;
    private final ReceiverRepository receiverRepository;
    private final AccessGrantRepository accessGrantRepository;
    private final ReceiverAcknowledgementRepository acknowledgementRepository;
    private final AuditLogService auditLogService;
    private final EncryptionService encryptionService;

    public List<ReceiverCapsuleResponse> getMyReleasedCapsules() {

        Users currentUser = currentUserService.getCurrentUser();

        validateReceiverRole(currentUser);

        String email = currentUser.getEmail().trim().toLowerCase();

        List<Receiver> receiverContacts = receiverRepository.findByEmailAndStatus(
                email,
                ReceiverStatus.ACTIVE
        );

        if (receiverContacts.isEmpty()) {
            return List.of();
        }

        return accessGrantRepository
                .findByReceiverInAndStatusOrderByGrantedAtDesc(
                        receiverContacts,
                        AccessGrantStatus.ACTIVE
                )
                .stream()
                .filter(this::isGrantForReleasedCapsule)
                .map(this::mapToListResponse)
                .toList();
    }

    public ReceiverCapsuleDetailResponse getReleasedCapsuleById(UUID capsuleId) {

        Users currentUser = currentUserService.getCurrentUser();

        validateReceiverRole(currentUser);

        String email = currentUser.getEmail().trim().toLowerCase();

        List<Receiver> receiverContacts = receiverRepository.findByEmailAndStatus(
                email,
                ReceiverStatus.ACTIVE
        );

        if (receiverContacts.isEmpty()) {
            throw new LegacyLockException("Capsule not found or access not granted");
        }

        AccessGrant accessGrant = accessGrantRepository
                .findByReceiverInAndStatusOrderByGrantedAtDesc(
                        receiverContacts,
                        AccessGrantStatus.ACTIVE
                )
                .stream()
                .filter(grant -> grant.getCapsule().getId().equals(capsuleId))
                .filter(this::isGrantForReleasedCapsule)
                .findFirst()
                .orElseThrow(() -> new LegacyLockException("Capsule not found or access not granted"));

        auditLogService.log(
                currentUser,
                AuditAction.CAPSULE_VIEWED_BY_RECEIVER,
                "CAPSULE",
                accessGrant.getCapsule().getId(),
                "Receiver viewed released capsule"
        );

        return mapToDetailResponse(accessGrant);
    }

    private boolean isGrantForReleasedCapsule(AccessGrant accessGrant) {
        return accessGrant.getCapsule().getStatus() == CapsuleStatus.RELEASED;
    }

    private void validateReceiverRole(Users user) {
        if (!user.getRoles().contains(Role.RECEIVER)) {
            throw new LegacyLockException("Only receivers can access released capsules");
        }
    }

    private ReceiverCapsuleResponse mapToListResponse(AccessGrant accessGrant) {

        Capsule capsule = accessGrant.getCapsule();
        Users owner = capsule.getOwner();

        var acknowledgement =
                acknowledgementRepository.findByAccessGrant(accessGrant);

        return ReceiverCapsuleResponse.builder()
                .accessGrantId(accessGrant.getId())
                .capsuleId(capsule.getId())
                .title(capsule.getTitle())
                .description(capsule.getDescription())
                .status(capsule.getStatus())
                .ownerName(owner.getName())
                .ownerEmail(owner.getEmail())
                .grantedAt(accessGrant.getGrantedAt())
                .acknowledged(acknowledgement.isPresent())
                .acknowledgedAt(
                        acknowledgement
                                .map(a -> a.getAcknowledgedAt())
                                .orElse(null)
                )
                .build();
    }

    private ReceiverCapsuleDetailResponse mapToDetailResponse(AccessGrant accessGrant) {

        Capsule capsule = accessGrant.getCapsule();
        Users owner = capsule.getOwner();

        return ReceiverCapsuleDetailResponse.builder()
                .accessGrantId(accessGrant.getId())
                .capsuleId(capsule.getId())
                .title(capsule.getTitle())
                .description(capsule.getDescription())
                .content(encryptionService.decrypt(capsule.getEncryptedContent()))
                .status(capsule.getStatus())
                .ownerName(owner.getName())
                .ownerEmail(owner.getEmail())
                .grantedAt(accessGrant.getGrantedAt())
                .build();
    }
}
