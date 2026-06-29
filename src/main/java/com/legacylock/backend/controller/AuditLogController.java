package com.legacylock.backend.controller;

import com.legacylock.backend.dto.response.AuditLogResponse;
import com.legacylock.backend.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AuditLogResponse>> getMyAuditLogs() {
        return ResponseEntity.ok(auditLogService.getMyAuditLogs());
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<List<AuditLogResponse>> getEntityAuditLogs(
            @PathVariable String entityType,
            @PathVariable UUID entityId
    ) {
        return ResponseEntity.ok(
                auditLogService.getEntityAuditLogs(entityType, entityId)
        );
    }
}
