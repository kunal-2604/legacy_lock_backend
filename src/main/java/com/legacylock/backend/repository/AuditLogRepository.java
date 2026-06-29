package com.legacylock.backend.repository;

import com.legacylock.backend.entity.AuditLog;
import com.legacylock.backend.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    List<AuditLog> findByActorUserOrderByCreatedAtDesc(Users actorUser);

    List<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
            String entityType,
            UUID entityId
    );
}
