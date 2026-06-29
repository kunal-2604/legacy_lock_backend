package com.legacylock.backend.dto.response;

import com.legacylock.backend.enums.CapsuleStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceiverCapsuleResponse {

    private UUID accessGrantId;

    private UUID capsuleId;
    private String title;
    private String description;
    private CapsuleStatus status;

    private String ownerName;
    private String ownerEmail;

    private LocalDateTime grantedAt;

    private boolean acknowledged;
    private LocalDateTime acknowledgedAt;
}
