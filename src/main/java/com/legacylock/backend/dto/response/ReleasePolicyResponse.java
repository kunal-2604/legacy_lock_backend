package com.legacylock.backend.dto.response;

import com.legacylock.backend.enums.ReleasePolicyStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReleasePolicyResponse {

    private UUID id;

    private UUID capsuleId;
    private String capsuleTitle;

    private Integer inactivityDays;
    private Integer graceDays;
    private Integer totalDaysBeforeRelease;

    private ReleasePolicyStatus status;

    private LocalDateTime firstReminderSentAt;
    private LocalDateTime secondReminderSentAt;
    private LocalDateTime finalReminderSentAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
