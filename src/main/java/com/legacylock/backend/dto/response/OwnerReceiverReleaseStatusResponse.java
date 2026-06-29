package com.legacylock.backend.dto.response;

import com.legacylock.backend.enums.AccessGrantStatus;
import com.legacylock.backend.enums.ReceiverStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OwnerReceiverReleaseStatusResponse {

    private UUID receiverId;
    private String receiverName;
    private String receiverEmail;
    private String receiverPhone;
    private ReceiverStatus receiverStatus;

    private boolean assigned;
    private LocalDateTime assignedAt;

    private boolean accessGranted;
    private UUID accessGrantId;
    private AccessGrantStatus accessGrantStatus;
    private LocalDateTime grantedAt;
    private LocalDateTime revokedAt;

    private boolean acknowledged;
    private UUID acknowledgementId;
    private String acknowledgementMessage;
    private LocalDateTime acknowledgedAt;
}
