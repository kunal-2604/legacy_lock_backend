package com.legacylock.backend.repository;

import com.legacylock.backend.entity.AccessGrant;
import com.legacylock.backend.entity.Capsule;
import com.legacylock.backend.entity.Receiver;
import com.legacylock.backend.enums.AccessGrantStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccessGrantRepository extends JpaRepository<AccessGrant, UUID> {

    boolean existsByCapsuleAndReceiver(
            Capsule capsule,
            Receiver receiver
    );

    List<AccessGrant> findByCapsuleOrderByGrantedAtDesc(
            Capsule capsule
    );

    Optional<AccessGrant> findByCapsuleAndReceiverAndStatus(
            Capsule capsule,
            Receiver receiver,
            AccessGrantStatus status
    );

    List<AccessGrant> findByReceiverInAndStatusOrderByGrantedAtDesc(
            List<Receiver> receivers,
            AccessGrantStatus status
    );

    Optional<AccessGrant> findByIdAndStatus(
            UUID id,
            AccessGrantStatus status
    );

    Optional<AccessGrant> findByCapsuleAndReceiver(
            Capsule capsule,
            Receiver receiver
    );

    boolean existsByCapsuleAndReceiver_EmailAndStatus(
            Capsule capsule,
            String receiverEmail,
            AccessGrantStatus status
    );

    Optional<AccessGrant> findByCapsuleAndReceiver_EmailAndStatus(
            Capsule capsule,
            String receiverEmail,
            AccessGrantStatus status
    );

    List<AccessGrant> findByCapsuleId(UUID capsuleId);
}
