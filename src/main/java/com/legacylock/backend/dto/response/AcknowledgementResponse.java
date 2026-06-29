package com.legacylock.backend.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcknowledgementResponse {

    private UUID id;

    private UUID accessGrantId;

    private UUID capsuleId;
    private String capsuleTitle;

    private UUID receiverUserId;
    private String receiverEmail;

    private String message;
    private LocalDateTime acknowledgedAt;
}
