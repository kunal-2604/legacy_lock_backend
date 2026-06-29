package com.legacylock.backend.service;

import com.legacylock.backend.dto.request.AcknowledgementRequest;
import com.legacylock.backend.dto.response.AcknowledgementResponse;
import com.legacylock.backend.entity.AccessGrant;
import com.legacylock.backend.entity.Capsule;
import com.legacylock.backend.entity.Receiver;
import com.legacylock.backend.entity.ReceiverAcknowledgement;
import com.legacylock.backend.entity.Users;
import com.legacylock.backend.enums.AccessGrantStatus;
import com.legacylock.backend.enums.AuditAction;
import com.legacylock.backend.enums.CapsuleStatus;
import com.legacylock.backend.enums.Role;
import com.legacylock.backend.exceptions.LegacyLockException;
import com.legacylock.backend.repository.AccessGrantRepository;
import com.legacylock.backend.repository.ReceiverAcknowledgementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ReceiverAcknowledgementService {

    private final CurrentUserService currentUserService;
    private final AccessGrantRepository accessGrantRepository;
    private final ReceiverAcknowledgementRepository acknowledgementRepository;
    private final AuditLogService auditLogService;

    public AcknowledgementResponse acknowledgeAccessGrant(
            UUID accessGrantId,
            AcknowledgementRequest request
    ) {
        Users currentUser = currentUserService.getCurrentUser();

        validateReceiverRole(currentUser);

        AccessGrant accessGrant = accessGrantRepository
                .findByIdAndStatus(accessGrantId, AccessGrantStatus.ACTIVE)
                .orElseThrow(() -> new LegacyLockException("Access grant not found"));

        validateAccessGrantBelongsToReceiver(accessGrant, currentUser);

        if (accessGrant.getCapsule().getStatus() != CapsuleStatus.RELEASED) {
            throw new LegacyLockException("Capsule is not released yet");
        }

        boolean alreadyAcknowledged =
                acknowledgementRepository.existsByAccessGrant(accessGrant);

        if (alreadyAcknowledged) {
            throw new LegacyLockException("Capsule already acknowledged");
        }

        ReceiverAcknowledgement acknowledgement =
                ReceiverAcknowledgement.builder()
                        .accessGrant(accessGrant)
                        .receiverUser(currentUser)
                        .message(normalizeOptionalText(request.getMessage()))
                        .build();

        ReceiverAcknowledgement saved =
                acknowledgementRepository.save(acknowledgement);

        auditLogService.log(
                currentUser,
                AuditAction.CAPSULE_ACKNOWLEDGED,
                "ACKNOWLEDGEMENT",
                saved.getId(),
                "Receiver acknowledged capsule " + saved.getAccessGrant().getCapsule().getTitle()
        );

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public AcknowledgementResponse getMyAcknowledgement(UUID accessGrantId) {
        Users currentUser = currentUserService.getCurrentUser();

        validateReceiverRole(currentUser);

        AccessGrant accessGrant = accessGrantRepository
                .findByIdAndStatus(accessGrantId, AccessGrantStatus.ACTIVE)
                .orElseThrow(() -> new LegacyLockException("Access grant not found"));

        validateAccessGrantBelongsToReceiver(accessGrant, currentUser);

        ReceiverAcknowledgement acknowledgement =
                acknowledgementRepository.findByAccessGrant(accessGrant)
                        .orElseThrow(() -> new LegacyLockException("Acknowledgement not found"));

        return mapToResponse(acknowledgement);
    }

    private void validateAccessGrantBelongsToReceiver(
            AccessGrant accessGrant,
            Users currentUser
    ) {
        Receiver receiver = accessGrant.getReceiver();

        String receiverContactEmail = receiver.getEmail().trim().toLowerCase();
        String currentUserEmail = currentUser.getEmail().trim().toLowerCase();

        if (!receiverContactEmail.equals(currentUserEmail)) {
            throw new LegacyLockException("Access grant not found");
        }
    }

    private void validateReceiverRole(Users user) {
        if (user.getRole() != Role.RECEIVER) {
            throw new LegacyLockException("Only receivers can acknowledge capsules");
        }
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return value.trim();
    }

    private AcknowledgementResponse mapToResponse(
            ReceiverAcknowledgement acknowledgement
    ) {
        AccessGrant accessGrant = acknowledgement.getAccessGrant();
        Capsule capsule = accessGrant.getCapsule();
        Users receiverUser = acknowledgement.getReceiverUser();

        return AcknowledgementResponse.builder()
                .id(acknowledgement.getId())
                .accessGrantId(accessGrant.getId())
                .capsuleId(capsule.getId())
                .capsuleTitle(capsule.getTitle())
                .receiverUserId(receiverUser.getId())
                .receiverEmail(receiverUser.getEmail())
                .message(acknowledgement.getMessage())
                .acknowledgedAt(acknowledgement.getAcknowledgedAt())
                .build();
    }
}
