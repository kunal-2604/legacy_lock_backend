package com.legacylock.backend.service;

import com.legacylock.backend.dto.response.OwnerCapsuleReleaseStatusResponse;
import com.legacylock.backend.dto.response.OwnerReceiverReleaseStatusResponse;
import com.legacylock.backend.entity.*;
import com.legacylock.backend.enums.CapsuleStatus;
import com.legacylock.backend.enums.Role;
import com.legacylock.backend.exceptions.LegacyLockException;
import com.legacylock.backend.repository.AccessGrantRepository;
import com.legacylock.backend.repository.CapsuleReceiverRepository;
import com.legacylock.backend.repository.CapsuleRepository;
import com.legacylock.backend.repository.ReceiverAcknowledgementRepository;
import com.legacylock.backend.repository.ReleasePolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OwnerReleaseStatusService {

    private final CurrentUserService currentUserService;
    private final CapsuleRepository capsuleRepository;
    private final CapsuleReceiverRepository capsuleReceiverRepository;
    private final AccessGrantRepository accessGrantRepository;
    private final ReceiverAcknowledgementRepository acknowledgementRepository;
    private final ReleasePolicyRepository releasePolicyRepository;

    public OwnerCapsuleReleaseStatusResponse getCapsuleReleaseStatus(UUID capsuleId) {

        Users owner = currentUserService.getCurrentUser();

        validateOwnerRole(owner);

        Capsule capsule = capsuleRepository.findByIdAndOwner(capsuleId, owner)
                .orElseThrow(() -> new LegacyLockException("Capsule not found"));

        if (capsule.getStatus() == CapsuleStatus.DELETED) {
            throw new LegacyLockException("Capsule not found");
        }

        ReleasePolicy releasePolicy = releasePolicyRepository.findByCapsule(capsule)
                .orElse(null);

        List<CapsuleReceiver> assignedReceivers =
                capsuleReceiverRepository.findByCapsuleOrderByAssignedAtDesc(capsule);

        List<OwnerReceiverReleaseStatusResponse> receiverStatuses =
                assignedReceivers.stream()
                        .map(this::mapReceiverStatus)
                        .toList();

        int totalAccessGrants = (int) receiverStatuses.stream()
                .filter(OwnerReceiverReleaseStatusResponse::isAccessGranted)
                .count();

        int totalAcknowledged = (int) receiverStatuses.stream()
                .filter(OwnerReceiverReleaseStatusResponse::isAcknowledged)
                .count();

        return OwnerCapsuleReleaseStatusResponse.builder()
                .capsuleId(capsule.getId())
                .capsuleTitle(capsule.getTitle())
                .capsuleDescription(capsule.getDescription())
                .capsuleStatus(capsule.getStatus())

                .releasePolicyId(releasePolicy != null ? releasePolicy.getId() : null)
                .releasePolicyStatus(releasePolicy != null ? releasePolicy.getStatus() : null)
                .inactivityDays(releasePolicy != null ? releasePolicy.getInactivityDays() : null)
                .graceDays(releasePolicy != null ? releasePolicy.getGraceDays() : null)

                .totalAssignedReceivers(assignedReceivers.size())
                .totalAccessGrants(totalAccessGrants)
                .totalAcknowledged(totalAcknowledged)

                .createdAt(capsule.getCreatedAt())
                .updatedAt(capsule.getUpdatedAt())

                .receivers(receiverStatuses)
                .build();
    }

    private OwnerReceiverReleaseStatusResponse mapReceiverStatus(
            CapsuleReceiver capsuleReceiver
    ) {
        Capsule capsule = capsuleReceiver.getCapsule();
        Receiver receiver = capsuleReceiver.getReceiver();

        Optional<AccessGrant> accessGrantOptional =
                accessGrantRepository.findByCapsuleAndReceiver(capsule, receiver);

        AccessGrant accessGrant = accessGrantOptional.orElse(null);

        ReceiverAcknowledgement acknowledgement = null;

        if (accessGrant != null) {
            acknowledgement = acknowledgementRepository
                    .findByAccessGrant(accessGrant)
                    .orElse(null);
        }

        return OwnerReceiverReleaseStatusResponse.builder()
                .receiverId(receiver.getId())
                .receiverName(receiver.getName())
                .receiverEmail(receiver.getEmail())
                .receiverPhone(receiver.getPhone())
                .receiverStatus(receiver.getStatus())

                .assigned(true)
                .assignedAt(capsuleReceiver.getAssignedAt())

                .accessGranted(accessGrant != null)
                .accessGrantId(accessGrant != null ? accessGrant.getId() : null)
                .accessGrantStatus(accessGrant != null ? accessGrant.getStatus() : null)
                .grantedAt(accessGrant != null ? accessGrant.getGrantedAt() : null)
                .revokedAt(accessGrant != null ? accessGrant.getRevokedAt() : null)

                .acknowledged(acknowledgement != null)
                .acknowledgementId(acknowledgement != null ? acknowledgement.getId() : null)
                .acknowledgementMessage(acknowledgement != null ? acknowledgement.getMessage() : null)
                .acknowledgedAt(acknowledgement != null ? acknowledgement.getAcknowledgedAt() : null)
                .build();
    }

    private void validateOwnerRole(Users user) {
        if (user.getRole() != Role.OWNER) {
            throw new LegacyLockException("Only owners can view release status");
        }
    }
}
