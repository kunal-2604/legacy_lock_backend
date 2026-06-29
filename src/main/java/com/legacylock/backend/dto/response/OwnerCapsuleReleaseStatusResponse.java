package com.legacylock.backend.dto.response;

import com.legacylock.backend.enums.CapsuleStatus;
import com.legacylock.backend.enums.ReleasePolicyStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OwnerCapsuleReleaseStatusResponse {

    private UUID capsuleId;
    private String capsuleTitle;
    private String capsuleDescription;
    private CapsuleStatus capsuleStatus;

    private UUID releasePolicyId;
    private ReleasePolicyStatus releasePolicyStatus;
    private Integer inactivityDays;
    private Integer graceDays;

    private int totalAssignedReceivers;
    private int totalAccessGrants;
    private int totalAcknowledged;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<OwnerReceiverReleaseStatusResponse> receivers;
}
