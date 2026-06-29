package com.legacylock.backend.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchedulerRunResponse {

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    private int totalPoliciesChecked;
    private int releasedCount;
    private int skippedCount;
    private int failedCount;

    private List<SchedulerPolicyResultResponse> results;
}
