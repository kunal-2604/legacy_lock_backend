package com.legacylock.backend.repository;

import com.legacylock.backend.entity.AccessGrant;
import com.legacylock.backend.entity.ReceiverAcknowledgement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReceiverAcknowledgementRepository
        extends JpaRepository<ReceiverAcknowledgement, UUID> {

    boolean existsByAccessGrant(AccessGrant accessGrant);

    Optional<ReceiverAcknowledgement> findByAccessGrant(AccessGrant accessGrant);
}
