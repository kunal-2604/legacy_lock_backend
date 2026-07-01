package com.legacylock.backend.service;

import com.legacylock.backend.dto.response.AuditLogResponse;
import com.legacylock.backend.entity.AuditLog;
import com.legacylock.backend.entity.Users;
import com.legacylock.backend.enums.AuditAction;
import com.legacylock.backend.enums.Role;
import com.legacylock.backend.exceptions.LegacyLockException;
import com.legacylock.backend.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final CurrentUserService currentUserService;

    @Transactional
    public void log(
            Users actorUser,
            AuditAction action,
            String entityType,
            UUID entityId,
            String details
    ) {
        AuditLog auditLog = AuditLog.builder()
                .actorUser(actorUser)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .build();

        auditLogRepository.save(auditLog);
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getMyAuditLogs() {
        Users currentUser = currentUserService.getCurrentUser();

        return auditLogRepository.findByActorUserOrderByCreatedAtDesc(currentUser)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getEntityAuditLogs(
            String entityType,
            UUID entityId
    ) {
        Users currentUser = currentUserService.getCurrentUser();

        if (!currentUser.getRoles().contains(Role.OWNER)
                && !currentUser.getRoles().contains(Role.ADMIN)) {
            throw new LegacyLockException("Not allowed to view audit logs");
        }

        return auditLogRepository
                .findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private AuditLogResponse mapToResponse(AuditLog auditLog) {
        Users actor = auditLog.getActorUser();

        return AuditLogResponse.builder()
                .id(auditLog.getId())
                .actorUserId(actor != null ? actor.getId() : null)
                .actorEmail(actor != null ? actor.getEmail() : null)
                .action(auditLog.getAction())
                .entityType(auditLog.getEntityType())
                .entityId(auditLog.getEntityId())
                .details(auditLog.getDetails())
                .createdAt(auditLog.getCreatedAt())
                .build();
    }
}
