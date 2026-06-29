package com.legacylock.backend.service;

import com.legacylock.backend.dto.response.CapsuleReceiverResponse;
import com.legacylock.backend.entity.Capsule;
import com.legacylock.backend.entity.CapsuleReceiver;
import com.legacylock.backend.entity.Receiver;
import com.legacylock.backend.entity.Users;
import com.legacylock.backend.enums.AuditAction;
import com.legacylock.backend.enums.CapsuleStatus;
import com.legacylock.backend.enums.ReceiverStatus;
import com.legacylock.backend.enums.Role;
import com.legacylock.backend.exceptions.LegacyLockException;
import com.legacylock.backend.repository.CapsuleReceiverRepository;
import com.legacylock.backend.repository.CapsuleRepository;
import com.legacylock.backend.repository.ReceiverRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CapsuleReceiverService {

    private final CapsuleReceiverRepository capsuleReceiverRepository;
    private final CapsuleRepository capsuleRepository;
    private final ReceiverRepository receiverRepository;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;

    public CapsuleReceiverResponse assignReceiverToCapsule(
            UUID capsuleId,
            UUID receiverId
    ) {
        Users owner = currentUserService.getCurrentUser();

        validateOwnerRole(owner);

        Capsule capsule = getOwnedCapsuleOrThrow(capsuleId, owner);
        Receiver receiver = getOwnedReceiverOrThrow(receiverId, owner);

        validateCapsuleCanBeAssigned(capsule);
        validateReceiverCanBeAssigned(receiver);

        boolean alreadyAssigned = capsuleReceiverRepository.existsByCapsuleAndReceiver(
                capsule,
                receiver
        );

        if (alreadyAssigned) {
            throw new LegacyLockException("Receiver is already assigned to this capsule");
        }

        CapsuleReceiver capsuleReceiver = CapsuleReceiver.builder()
                .capsule(capsule)
                .receiver(receiver)
                .build();

        CapsuleReceiver savedAssignment =
                capsuleReceiverRepository.save(capsuleReceiver);

        auditLogService.log(
                owner,
                AuditAction.RECEIVER_ASSIGNED_TO_CAPSULE,
                "CAPSULE_RECEIVER",
                savedAssignment.getId(),
                "Receiver " + receiver.getEmail() + " assigned to capsule " + capsule.getTitle()
        );

        return mapToResponse(savedAssignment);
    }

    @Transactional(readOnly = true)
    public List<CapsuleReceiverResponse> getAssignedReceivers(UUID capsuleId) {
        Users owner = currentUserService.getCurrentUser();

        validateOwnerRole(owner);

        Capsule capsule = getOwnedCapsuleOrThrow(capsuleId, owner);

        if (capsule.getStatus() == CapsuleStatus.DELETED) {
            throw new LegacyLockException("Capsule not found");
        }

        return capsuleReceiverRepository.findByCapsuleOrderByAssignedAtDesc(capsule)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public void removeReceiverFromCapsule(
            UUID capsuleId,
            UUID receiverId
    ) {
        Users owner = currentUserService.getCurrentUser();

        validateOwnerRole(owner);

        Capsule capsule = getOwnedCapsuleOrThrow(capsuleId, owner);
        Receiver receiver = getOwnedReceiverOrThrow(receiverId, owner);

        if (capsule.getStatus() == CapsuleStatus.DELETED) {
            throw new LegacyLockException("Capsule not found");
        }

        if (capsule.getStatus() == CapsuleStatus.RELEASED) {
            throw new LegacyLockException("Cannot remove receiver from released capsule");
        }

        CapsuleReceiver assignment = capsuleReceiverRepository
                .findByCapsuleAndReceiver(capsule, receiver)
                .orElseThrow(() -> new LegacyLockException("Receiver is not assigned to this capsule"));

        capsuleReceiverRepository.delete(assignment);

        auditLogService.log(
                owner,
                AuditAction.RECEIVER_REMOVED_FROM_CAPSULE,
                "CAPSULE_RECEIVER",
                assignment.getId(),
                "Receiver " + receiver.getEmail() + " removed from capsule " + capsule.getTitle()
        );
    }

    private Capsule getOwnedCapsuleOrThrow(UUID capsuleId, Users owner) {
        return capsuleRepository.findByIdAndOwner(capsuleId, owner)
                .orElseThrow(() -> new LegacyLockException("Capsule not found"));
    }

    private Receiver getOwnedReceiverOrThrow(UUID receiverId, Users owner) {
        return receiverRepository.findByIdAndOwner(receiverId, owner)
                .orElseThrow(() -> new LegacyLockException("Receiver not found"));
    }

    private void validateCapsuleCanBeAssigned(Capsule capsule) {
        if (capsule.getStatus() == CapsuleStatus.DELETED) {
            throw new LegacyLockException("Capsule not found");
        }

        if (capsule.getStatus() == CapsuleStatus.RELEASED) {
            throw new LegacyLockException("Cannot assign receiver to released capsule");
        }
    }

    private void validateReceiverCanBeAssigned(Receiver receiver) {
        if (receiver.getStatus() == ReceiverStatus.REMOVED) {
            throw new LegacyLockException("Receiver not found");
        }
    }

    private void validateOwnerRole(Users user) {
        if (user.getRole() != Role.OWNER) {
            throw new LegacyLockException("Only owners can assign receivers to capsules");
        }
    }

    private CapsuleReceiverResponse mapToResponse(CapsuleReceiver capsuleReceiver) {
        Capsule capsule = capsuleReceiver.getCapsule();
        Receiver receiver = capsuleReceiver.getReceiver();

        return CapsuleReceiverResponse.builder()
                .assignmentId(capsuleReceiver.getId())
                .capsuleId(capsule.getId())
                .capsuleTitle(capsule.getTitle())
                .receiverId(receiver.getId())
                .receiverName(receiver.getName())
                .receiverEmail(receiver.getEmail())
                .receiverPhone(receiver.getPhone())
                .assignedAt(capsuleReceiver.getAssignedAt())
                .build();
    }
}
