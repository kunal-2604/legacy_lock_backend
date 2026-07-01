package com.legacylock.backend.repository;

import com.legacylock.backend.entity.Receiver;
import com.legacylock.backend.entity.Users;
import com.legacylock.backend.enums.ReceiverStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReceiverRepository extends JpaRepository<Receiver, UUID> {

    List<Receiver> findByOwnerAndStatusOrderByCreatedAtDesc(
            Users owner,
            ReceiverStatus status
    );

    Optional<Receiver> findByIdAndOwner(UUID id, Users owner);

    boolean existsByOwnerAndEmailAndStatus(
            Users owner,
            String email,
            ReceiverStatus status
    );

    List<Receiver> findByEmailAndStatus(
            String email,
            ReceiverStatus status
    );

    Optional<Receiver> findByOwnerAndEmailIgnoreCase(Users owner, String email);

    Optional<Receiver> findByOwnerIdAndEmailIgnoreCase(UUID ownerId, String email);
}
