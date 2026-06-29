package com.legacylock.backend.dto.response;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchedulerPolicyResultResponse {

    private UUID policyId;
    private UUID capsuleId;
    private String capsuleTitle;

    private String result;
    private String reason;

    private Integer inactivityDays;
    private Integer graceDays;

    private String latestCheckInAt;
    private String releaseAt;
    private String checkedAt;

    private int assignedReceivers;
    private int grantsCreated;
}
