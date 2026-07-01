package com.legacylock.backend.service;

import com.legacylock.backend.dto.response.SchedulerPolicyResultResponse;
import com.legacylock.backend.dto.response.SchedulerRunResponse;
import com.legacylock.backend.entity.*;
import com.legacylock.backend.enums.*;
import com.legacylock.backend.repository.AccessGrantRepository;
import com.legacylock.backend.repository.CapsuleReceiverRepository;
import com.legacylock.backend.repository.CapsuleRepository;
import com.legacylock.backend.repository.CheckInRepository;
import com.legacylock.backend.repository.ReleasePolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReleaseSchedulerService {

    private final ReleasePolicyRepository releasePolicyRepository;
    private final CheckInRepository checkInRepository;
    private final CapsuleReceiverRepository capsuleReceiverRepository;
    private final AccessGrantRepository accessGrantRepository;
    private final CapsuleRepository capsuleRepository;
    private final AuditLogService auditLogService;
    private final EmailService emailService;

    /**
     * Runs every 1 minute for development.
     * Later we can change this to hourly/daily.
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void scheduledReleaseCheck() {
        runReleaseCheck();
    }

    /**
     * This method can be used by scheduler and dev trigger endpoint.
     */
    @Transactional
    public SchedulerRunResponse runReleaseCheck() {

        LocalDateTime startedAt = LocalDateTime.now();

        log.info("Running release scheduler...");

        List<ReleasePolicy> activePolicies =
                releasePolicyRepository.findAllByStatusWithCapsuleAndOwner(ReleasePolicyStatus.ACTIVE);

        List<SchedulerPolicyResultResponse> results = new ArrayList<>();

        for (ReleasePolicy policy : activePolicies) {

            UUID policyId = policy.getId();

            Capsule capsule = policy.getCapsule();
            UUID capsuleId = capsule.getId();
            String capsuleTitle = capsule.getTitle();

            try {
                log.info(
                        "Checking policy {} for capsule {}",
                        policyId,
                        capsuleId
                );

                SchedulerPolicyResultResponse result = processPolicy(policy);

                results.add(result);

            } catch (Exception e) {
                log.error(
                        "Failed to process release policy {} for capsule {}: {}",
                        policyId,
                        capsuleId,
                        e.getMessage(),
                        e
                );

                results.add(
                        SchedulerPolicyResultResponse.builder()
                                .policyId(policyId)
                                .capsuleId(capsuleId)
                                .capsuleTitle(capsuleTitle)
                                .result("FAILED")
                                .checkedAt(LocalDateTime.now().toString())
                                .reason(e.getMessage())
                                .build()
                );
            }
        }

        int releasedCount = (int) results.stream()
                .filter(result -> "RELEASED".equals(result.getResult()))
                .count();

        int skippedCount = (int) results.stream()
                .filter(result -> "SKIPPED".equals(result.getResult()))
                .count();

        int failedCount = (int) results.stream()
                .filter(result -> "FAILED".equals(result.getResult()))
                .count();

        LocalDateTime completedAt = LocalDateTime.now();

        log.info(
                "Release scheduler completed. checked={}, released={}, skipped={}, failed={}",
                activePolicies.size(),
                releasedCount,
                skippedCount,
                failedCount
        );

        return SchedulerRunResponse.builder()
                .startedAt(startedAt)
                .completedAt(completedAt)
                .totalPoliciesChecked(activePolicies.size())
                .releasedCount(releasedCount)
                .skippedCount(skippedCount)
                .failedCount(failedCount)
                .results(results)
                .build();
    }

    private SchedulerPolicyResultResponse processPolicy(ReleasePolicy policy) {

        LocalDateTime now = LocalDateTime.now();

        Capsule capsule = policy.getCapsule();

        log.info(
                "Checking policy {} for capsule {}",
                policy.getId(),
                capsule.getId()
        );

        if (capsule.getStatus() != CapsuleStatus.ACTIVE) {
            return skipped(
                    policy,
                    "Capsule status is " + capsule.getStatus(),
                    now,
                    null,
                    null,
                    0,
                    0
            );
        }

        Users owner = capsule.getOwner();

        CheckIn latestCheckIn = checkInRepository
                .findFirstByOwnerOrderByCheckedInAtDesc(owner)
                .orElse(null);

        LocalDateTime lastActivityAt = latestCheckIn != null
                ? latestCheckIn.getCheckedInAt()
                : capsule.getCreatedAt();

        LocalDateTime inactivityDeadline = lastActivityAt
                .plusDays(policy.getInactivityDays());

        LocalDateTime releaseAt = inactivityDeadline
                .plusDays(policy.getGraceDays());

        if (now.isBefore(inactivityDeadline)) {
            return skipped(
                    policy,
                    "Owner is still within inactivity period",
                    now,
                    lastActivityAt,
                    releaseAt,
                    0,
                    0
            );
        }

        if (now.isBefore(releaseAt)) {
            boolean reminderSent = sendDueReminderIfNeeded(
                    policy,
                    capsule,
                    owner,
                    lastActivityAt,
                    inactivityDeadline,
                    releaseAt,
                    now
            );

            if (reminderSent) {
                releasePolicyRepository.save(policy);
            }

            return skipped(
                    policy,
                    reminderSent
                            ? "Reminder email sent to owner"
                            : "Capsule is in grace period. No reminder due now",
                    now,
                    lastActivityAt,
                    releaseAt,
                    0,
                    0
            );
        }

        List<CapsuleReceiver> assignedReceivers =
                capsuleReceiverRepository.findByCapsuleWithReceiverOrderByAssignedAtDesc(capsule);

        if (assignedReceivers.isEmpty()) {
            return skipped(
                    policy,
                    "Capsule has no assigned receivers",
                    now,
                    latestCheckIn.getCheckedInAt(),
                    releaseAt,
                    0,
                    0
            );
        }

        int grantsCreated = createAccessGrants(capsule, assignedReceivers);

        if (grantsCreated == 0) {
            return skipped(
                    policy,
                    "No grants created. Receivers may be removed or grants already exist",
                    now,
                    latestCheckIn.getCheckedInAt(),
                    releaseAt,
                    assignedReceivers.size(),
                    0
            );
        }

        capsule.setStatus(CapsuleStatus.RELEASED);
        capsuleRepository.save(capsule);

        policy.setStatus(ReleasePolicyStatus.COMPLETED);
        releasePolicyRepository.save(policy);

        log.info(
                "Capsule {} released successfully. Grants created: {}",
                capsule.getId(),
                grantsCreated
        );

        auditLogService.log(
                capsule.getOwner(),
                AuditAction.CAPSULE_RELEASED,
                "CAPSULE",
                capsule.getId(),
                "Capsule released by scheduler. Grants created: " + grantsCreated
        );

        return SchedulerPolicyResultResponse.builder()
                .policyId(policy.getId())
                .capsuleId(capsule.getId())
                .capsuleTitle(capsule.getTitle())
                .result("RELEASED")
                .reason("Capsule released successfully")
                .inactivityDays(policy.getInactivityDays())
                .graceDays(policy.getGraceDays())
                .latestCheckInAt(lastActivityAt.toString())
                .releaseAt(releaseAt.toString())
                .checkedAt(now.toString())
                .assignedReceivers(assignedReceivers.size())
                .grantsCreated(grantsCreated)
                .build();
    }

    private int createAccessGrants(
            Capsule capsule,
            List<CapsuleReceiver> assignedReceivers
    ) {
        int createdGrantCount = 0;

        for (CapsuleReceiver capsuleReceiver : assignedReceivers) {

            Receiver receiver = capsuleReceiver.getReceiver();

            if (receiver.getStatus() != ReceiverStatus.ACTIVE) {
                log.info(
                        "Skipping receiver {} because status is {}",
                        receiver.getId(),
                        receiver.getStatus()
                );
                continue;
            }

            boolean grantAlreadyExists =
                    accessGrantRepository.existsByCapsuleAndReceiver(capsule, receiver);

            if (grantAlreadyExists) {
                log.info(
                        "Access grant already exists for capsule {} and receiver {}",
                        capsule.getId(),
                        receiver.getId()
                );
                continue;
            }

            AccessGrant accessGrant = AccessGrant.builder()
                    .capsule(capsule)
                    .receiver(receiver)
                    .status(AccessGrantStatus.ACTIVE)
                    .build();

            AccessGrant savedGrant = accessGrantRepository.save(accessGrant);

            auditLogService.log(
                    capsule.getOwner(),
                    AuditAction.ACCESS_GRANTED,
                    "ACCESS_GRANT",
                    savedGrant.getId(),
                    "Access granted to receiver " + receiver.getEmail()
            );

            createdGrantCount++;
        }

        return createdGrantCount;
    }

    private boolean sendDueReminderIfNeeded(
            ReleasePolicy policy,
            Capsule capsule,
            Users owner,
            LocalDateTime lastActivityAt,
            LocalDateTime inactivityDeadline,
            LocalDateTime releaseAt,
            LocalDateTime now
    ) {
        if (policy.getGraceDays() == null || policy.getGraceDays() <= 0) {
            return false;
        }

        LocalDateTime secondReminderAt = calculateSecondReminderAt(
                inactivityDeadline,
                policy.getGraceDays()
        );

        LocalDateTime finalReminderAt = releaseAt.minusDays(1);

        if (policy.getFirstReminderSentAt() == null
                && !now.isBefore(inactivityDeadline)) {

            sendOwnerReminderEmail(
                    policy,
                    capsule,
                    owner,
                    lastActivityAt,
                    releaseAt,
                    "FIRST"
            );

            policy.setFirstReminderSentAt(now);
            return true;
        }

        if (policy.getSecondReminderSentAt() == null
                && !now.isBefore(secondReminderAt)) {

            sendOwnerReminderEmail(
                    policy,
                    capsule,
                    owner,
                    lastActivityAt,
                    releaseAt,
                    "SECOND"
            );

            policy.setSecondReminderSentAt(now);
            return true;
        }

        if (policy.getFinalReminderSentAt() == null
                && !now.isBefore(finalReminderAt)) {

            sendOwnerReminderEmail(
                    policy,
                    capsule,
                    owner,
                    lastActivityAt,
                    releaseAt,
                    "FINAL"
            );

            policy.setFinalReminderSentAt(now);
            return true;
        }

        return false;
    }

    private LocalDateTime calculateSecondReminderAt(
            LocalDateTime inactivityDeadline,
            Integer graceDays
    ) {
        int halfGraceDays = Math.max(1, graceDays / 2);
        return inactivityDeadline.plusDays(halfGraceDays);
    }

    private void sendOwnerReminderEmail(
            ReleasePolicy policy,
            Capsule capsule,
            Users owner,
            LocalDateTime lastActivityAt,
            LocalDateTime releaseAt,
            String reminderType
    ) {
        String subject = switch (reminderType) {
            case "FIRST" -> "LegacyLock Reminder: Please check in";
            case "SECOND" -> "LegacyLock Reminder: Your capsule is still pending release";
            case "FINAL" -> "LegacyLock Final Reminder: Capsule release is near";
            default -> "LegacyLock Reminder";
        };

        String body = """
            Hello %s,

            This is a LegacyLock safety reminder.

            We have not detected a check-in for your account.

            Capsule:
            %s

            Last activity:
            %s

            Scheduled release time:
            %s

            Please log in and check in if you are safe and active.
            If you check in before the release time, your capsule will not be released.

            Regards,
            LegacyLock
            """.formatted(
                owner.getName(),
                capsule.getTitle(),
                lastActivityAt,
                releaseAt
        );

        emailService.sendSimpleEmail(
                owner.getEmail(),
                subject,
                body
        );

        auditLogService.log(
                owner,
                AuditAction.OWNER_REMINDER_EMAIL_SENT,
                "RELEASE_POLICY",
                policy.getId(),
                reminderType + " reminder email sent for capsule " + capsule.getTitle()
        );

        log.info(
                "{} reminder email sent to owner {} for capsule {}",
                reminderType,
                owner.getEmail(),
                capsule.getId()
        );
    }

    private SchedulerPolicyResultResponse skipped(
            ReleasePolicy policy,
            String reason,
            LocalDateTime checkedAt,
            LocalDateTime latestCheckInAt,
            LocalDateTime releaseAt,
            int assignedReceivers,
            int grantsCreated
    ) {
        Capsule capsule = policy.getCapsule();

        log.info(
                "Skipping capsule {}. Reason: {}",
                capsule.getId(),
                reason
        );

        return SchedulerPolicyResultResponse.builder()
                .policyId(policy.getId())
                .capsuleId(capsule.getId())
                .capsuleTitle(capsule.getTitle())
                .result("SKIPPED")
                .reason(reason)
                .inactivityDays(policy.getInactivityDays())
                .graceDays(policy.getGraceDays())
                .latestCheckInAt(latestCheckInAt != null ? latestCheckInAt.toString() : null)
                .releaseAt(releaseAt != null ? releaseAt.toString() : null)
                .checkedAt(checkedAt.toString())
                .assignedReceivers(assignedReceivers)
                .grantsCreated(grantsCreated)
                .build();
    }
}
