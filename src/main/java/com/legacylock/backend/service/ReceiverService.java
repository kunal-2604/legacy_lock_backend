package com.legacylock.backend.service;

import com.legacylock.backend.dto.request.ReceiverCreateRequest;
import com.legacylock.backend.dto.request.ReceiverUpdateRequest;
import com.legacylock.backend.dto.response.ReceiverResponse;
import com.legacylock.backend.entity.Receiver;
import com.legacylock.backend.entity.Users;
import com.legacylock.backend.enums.AuditAction;
import com.legacylock.backend.enums.ReceiverStatus;
import com.legacylock.backend.enums.Role;
import com.legacylock.backend.exceptions.LegacyLockException;
import com.legacylock.backend.repository.ReceiverRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ReceiverService {

    private final ReceiverRepository receiverRepository;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;

    public ReceiverResponse createReceiver(ReceiverCreateRequest request) {

        Users owner = currentUserService.getCurrentUser();

        validateOwnerRole(owner);

        String normalizedEmail = request.getEmail().trim().toLowerCase();

        boolean alreadyExists = receiverRepository.existsByOwnerAndEmailAndStatus(
                owner,
                normalizedEmail,
                ReceiverStatus.ACTIVE
        );

        if (alreadyExists) {
            throw new LegacyLockException("Receiver with this email already exists");
        }

        Receiver receiver = Receiver.builder()
                .owner(owner)
                .name(request.getName().trim())
                .email(normalizedEmail)
                .phone(request.getPhone())
                .status(ReceiverStatus.ACTIVE)
                .build();

        Receiver savedReceiver = receiverRepository.save(receiver);

        auditLogService.log(
                owner,
                AuditAction.RECEIVER_CREATED,
                "RECEIVER",
                savedReceiver.getId(),
                "Receiver created: " + savedReceiver.getEmail()
        );

        return mapToResponse(savedReceiver);
    }

    @Transactional(readOnly = true)
    public List<ReceiverResponse> getMyReceivers() {

        Users owner = currentUserService.getCurrentUser();

        validateOwnerRole(owner);

        return receiverRepository
                .findByOwnerAndStatusOrderByCreatedAtDesc(owner, ReceiverStatus.ACTIVE)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReceiverResponse getReceiverById(UUID receiverId) {

        Users owner = currentUserService.getCurrentUser();

        validateOwnerRole(owner);

        Receiver receiver = receiverRepository.findByIdAndOwner(receiverId, owner)
                .orElseThrow(() -> new LegacyLockException("Receiver not found"));

        if (receiver.getStatus() == ReceiverStatus.REMOVED) {
            throw new LegacyLockException("Receiver not found");
        }

        return mapToResponse(receiver);
    }

    public ReceiverResponse updateReceiver(
            UUID receiverId,
            ReceiverUpdateRequest request
    ) {

        Users owner = currentUserService.getCurrentUser();

        validateOwnerRole(owner);

        Receiver receiver = receiverRepository.findByIdAndOwner(receiverId, owner)
                .orElseThrow(() -> new LegacyLockException("Receiver not found"));

        if (receiver.getStatus() == ReceiverStatus.REMOVED) {
            throw new LegacyLockException("Receiver not found");
        }

        String normalizedEmail = request.getEmail().trim().toLowerCase();

        boolean emailChanged = !receiver.getEmail().equals(normalizedEmail);

        if (emailChanged) {
            boolean emailAlreadyUsed = receiverRepository.existsByOwnerAndEmailAndStatus(
                    owner,
                    normalizedEmail,
                    ReceiverStatus.ACTIVE
            );

            if (emailAlreadyUsed) {
                throw new LegacyLockException("Receiver with this email already exists");
            }
        }

        receiver.setName(request.getName().trim());
        receiver.setEmail(normalizedEmail);
        receiver.setPhone(request.getPhone());

        Receiver updatedReceiver = receiverRepository.save(receiver);

        auditLogService.log(
                owner,
                AuditAction.RECEIVER_UPDATED,
                "RECEIVER",
                updatedReceiver.getId(),
                "Receiver updated: " + updatedReceiver.getEmail()
        );

        return mapToResponse(updatedReceiver);
    }

    public void removeReceiver(UUID receiverId) {

        Users owner = currentUserService.getCurrentUser();

        validateOwnerRole(owner);

        Receiver receiver = receiverRepository.findByIdAndOwner(receiverId, owner)
                .orElseThrow(() -> new LegacyLockException("Receiver not found"));

        if (receiver.getStatus() == ReceiverStatus.REMOVED) {
            throw new LegacyLockException("Receiver already removed");
        }

        receiver.setStatus(ReceiverStatus.REMOVED);

        receiverRepository.save(receiver);

        auditLogService.log(
                owner,
                AuditAction.RECEIVER_REMOVED,
                "RECEIVER",
                receiver.getId(),
                "Receiver removed: " + receiver.getEmail()
        );
    }

    private void validateOwnerRole(Users user) {
        if (!user.getRoles().contains(Role.OWNER)) {
            throw new LegacyLockException("Only owners can manage receivers");
        }
    }

    private ReceiverResponse mapToResponse(Receiver receiver) {
        return ReceiverResponse.builder()
                .id(receiver.getId())
                .name(receiver.getName())
                .email(receiver.getEmail())
                .phone(receiver.getPhone())
                .status(receiver.getStatus())
                .createdAt(receiver.getCreatedAt())
                .updatedAt(receiver.getUpdatedAt())
                .build();
    }
}
