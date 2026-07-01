package com.legacylock.backend.service;

import com.legacylock.backend.dto.request.ReleasePolicyCreateRequest;
import com.legacylock.backend.dto.request.ReleasePolicyUpdateRequest;
import com.legacylock.backend.dto.response.ReleasePolicyResponse;
import com.legacylock.backend.entity.Capsule;
import com.legacylock.backend.entity.ReleasePolicy;
import com.legacylock.backend.entity.Users;
import com.legacylock.backend.enums.AuditAction;
import com.legacylock.backend.enums.CapsuleStatus;
import com.legacylock.backend.enums.ReleasePolicyStatus;
import com.legacylock.backend.enums.Role;
import com.legacylock.backend.exceptions.LegacyLockException;
import com.legacylock.backend.repository.CapsuleRepository;
import com.legacylock.backend.repository.ReleasePolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ReleasePolicyService {

    private final ReleasePolicyRepository releasePolicyRepository;
    private final CapsuleRepository capsuleRepository;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;

    public ReleasePolicyResponse createPolicy(
            UUID capsuleId,
            ReleasePolicyCreateRequest request
    ) {
        Users owner = currentUserService.getCurrentUser();

        validateOwnerRole(owner);

        Capsule capsule = getOwnedCapsuleOrThrow(capsuleId, owner);

        validateCapsuleCanHavePolicy(capsule);

        boolean policyAlreadyExists = releasePolicyRepository.existsByCapsule(capsule);

        if (policyAlreadyExists) {
            throw new LegacyLockException("Release policy already exists for this capsule");
        }

        ReleasePolicy releasePolicy = ReleasePolicy.builder()
                .capsule(capsule)
                .inactivityDays(request.getInactivityDays())
                .graceDays(request.getGraceDays())
                .status(ReleasePolicyStatus.ACTIVE)
                .build();

        ReleasePolicy savedPolicy = releasePolicyRepository.save(releasePolicy);

        auditLogService.log(
                owner,
                AuditAction.RELEASE_POLICY_CREATED,
                "RELEASE_POLICY",
                savedPolicy.getId(),
                "Release policy created for capsule " + capsule.getTitle()
        );

        return mapToResponse(savedPolicy);
    }

    @Transactional(readOnly = true)
    public ReleasePolicyResponse getPolicy(UUID capsuleId) {
        Users owner = currentUserService.getCurrentUser();

        validateOwnerRole(owner);

        Capsule capsule = getOwnedCapsuleOrThrow(capsuleId, owner);

        if (capsule.getStatus() == CapsuleStatus.DELETED) {
            throw new LegacyLockException("Capsule not found");
        }

        ReleasePolicy releasePolicy = releasePolicyRepository.findByCapsule(capsule)
                .orElseThrow(() -> new LegacyLockException("Release policy not found"));

        return mapToResponse(releasePolicy);
    }

    public ReleasePolicyResponse updatePolicy(
            UUID capsuleId,
            ReleasePolicyUpdateRequest request
    ) {
        Users owner = currentUserService.getCurrentUser();

        validateOwnerRole(owner);

        Capsule capsule = getOwnedCapsuleOrThrow(capsuleId, owner);

        if (capsule.getStatus() == CapsuleStatus.DELETED) {
            throw new LegacyLockException("Capsule not found");
        }

        if (capsule.getStatus() == CapsuleStatus.RELEASED) {
            throw new LegacyLockException("Cannot update policy of released capsule");
        }

        ReleasePolicy releasePolicy = releasePolicyRepository.findByCapsule(capsule)
                .orElseThrow(() -> new LegacyLockException("Release policy not found"));

        if (releasePolicy.getStatus() == ReleasePolicyStatus.COMPLETED) {
            throw new LegacyLockException("Completed release policy cannot be updated");
        }

        releasePolicy.setInactivityDays(request.getInactivityDays());
        releasePolicy.setGraceDays(request.getGraceDays());

        ReleasePolicy updatedPolicy = releasePolicyRepository.save(releasePolicy);

        auditLogService.log(
                owner,
                AuditAction.RELEASE_POLICY_UPDATED,
                "RELEASE_POLICY",
                updatedPolicy.getId(),
                "Release policy updated for capsule " + capsule.getTitle()
        );

        return mapToResponse(updatedPolicy);
    }

    public ReleasePolicyResponse pausePolicy(UUID capsuleId) {
        Users owner = currentUserService.getCurrentUser();

        validateOwnerRole(owner);

        Capsule capsule = getOwnedCapsuleOrThrow(capsuleId, owner);

        if (capsule.getStatus() == CapsuleStatus.DELETED) {
            throw new LegacyLockException("Capsule not found");
        }

        if (capsule.getStatus() == CapsuleStatus.RELEASED) {
            throw new LegacyLockException("Cannot pause policy of released capsule");
        }

        ReleasePolicy releasePolicy = releasePolicyRepository.findByCapsule(capsule)
                .orElseThrow(() -> new LegacyLockException("Release policy not found"));

        if (releasePolicy.getStatus() == ReleasePolicyStatus.COMPLETED) {
            throw new LegacyLockException("Completed release policy cannot be paused");
        }

        releasePolicy.setStatus(ReleasePolicyStatus.PAUSED);

        ReleasePolicy pausedPolicy = releasePolicyRepository.save(releasePolicy);

        auditLogService.log(
                owner,
                AuditAction.RELEASE_POLICY_PAUSED,
                "RELEASE_POLICY",
                pausedPolicy.getId(),
                "Release policy paused for capsule " + capsule.getTitle()
        );

        return mapToResponse(pausedPolicy);
    }

    public ReleasePolicyResponse activatePolicy(UUID capsuleId) {
        Users owner = currentUserService.getCurrentUser();

        validateOwnerRole(owner);

        Capsule capsule = getOwnedCapsuleOrThrow(capsuleId, owner);

        if (capsule.getStatus() == CapsuleStatus.DELETED) {
            throw new LegacyLockException("Capsule not found");
        }

        if (capsule.getStatus() == CapsuleStatus.RELEASED) {
            throw new LegacyLockException("Cannot activate policy of released capsule");
        }

        ReleasePolicy releasePolicy = releasePolicyRepository.findByCapsule(capsule)
                .orElseThrow(() -> new LegacyLockException("Release policy not found"));

        if (releasePolicy.getStatus() == ReleasePolicyStatus.COMPLETED) {
            throw new LegacyLockException("Completed release policy cannot be activated");
        }

        releasePolicy.setStatus(ReleasePolicyStatus.ACTIVE);

        ReleasePolicy activatedPolicy = releasePolicyRepository.save(releasePolicy);

        auditLogService.log(
                owner,
                AuditAction.RELEASE_POLICY_ACTIVATED,
                "RELEASE_POLICY",
                activatedPolicy.getId(),
                "Release policy activated for capsule " + capsule.getTitle()
        );

        return mapToResponse(activatedPolicy);
    }

    private Capsule getOwnedCapsuleOrThrow(UUID capsuleId, Users owner) {
        return capsuleRepository.findByIdAndOwner(capsuleId, owner)
                .orElseThrow(() -> new LegacyLockException("Capsule not found"));
    }

    private void validateCapsuleCanHavePolicy(Capsule capsule) {
        if (capsule.getStatus() == CapsuleStatus.DELETED) {
            throw new LegacyLockException("Capsule not found");
        }

        if (capsule.getStatus() == CapsuleStatus.RELEASED) {
            throw new LegacyLockException("Cannot create policy for released capsule");
        }
    }

    private void validateOwnerRole(Users user) {
        if (!user.getRoles().contains(Role.OWNER)) {
            throw new LegacyLockException("Only owners can manage release policies");
        }
    }

    private ReleasePolicyResponse mapToResponse(ReleasePolicy policy) {
        return ReleasePolicyResponse.builder()
                .id(policy.getId())
                .capsuleId(policy.getCapsule().getId())
                .capsuleTitle(policy.getCapsule().getTitle())
                .inactivityDays(policy.getInactivityDays())
                .graceDays(policy.getGraceDays())
                .totalDaysBeforeRelease(
                        policy.getInactivityDays() + policy.getGraceDays()
                )
                .status(policy.getStatus())
                .firstReminderSentAt(policy.getFirstReminderSentAt())
                .secondReminderSentAt(policy.getSecondReminderSentAt())
                .finalReminderSentAt(policy.getFinalReminderSentAt())
                .createdAt(policy.getCreatedAt())
                .updatedAt(policy.getUpdatedAt())
                .build();
    }
}
