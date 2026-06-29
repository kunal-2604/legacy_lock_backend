package com.legacylock.backend.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CapsuleReceiverResponse {

    private UUID assignmentId;

    private UUID capsuleId;
    private String capsuleTitle;

    private UUID receiverId;
    private String receiverName;
    private String receiverEmail;
    private String receiverPhone;

    private LocalDateTime assignedAt;
}
