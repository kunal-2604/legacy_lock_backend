package com.legacylock.backend.dto.response;

import com.legacylock.backend.enums.AuditAction;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogResponse {

    private UUID id;

    private UUID actorUserId;
    private String actorEmail;

    private AuditAction action;

    private String entityType;
    private UUID entityId;

    private String details;

    private LocalDateTime createdAt;
}
