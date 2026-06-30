package com.legacylock.backend.repository;

import com.legacylock.backend.entity.Capsule;
import com.legacylock.backend.entity.CapsuleReceiver;
import com.legacylock.backend.entity.Receiver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CapsuleReceiverRepository extends JpaRepository<CapsuleReceiver, UUID> {

    boolean existsByCapsuleAndReceiver(Capsule capsule, Receiver receiver);

    List<CapsuleReceiver> findByCapsuleOrderByAssignedAtDesc(Capsule capsule);

    Optional<CapsuleReceiver> findByCapsuleAndReceiver(
            Capsule capsule,
            Receiver receiver
    );

    @Query("""
        SELECT cr
        FROM CapsuleReceiver cr
        JOIN FETCH cr.receiver r
        WHERE cr.capsule = :capsule
        ORDER BY cr.assignedAt DESC
        """)
    List<CapsuleReceiver> findByCapsuleWithReceiverOrderByAssignedAtDesc(Capsule capsule);

    void deleteByCapsuleAndReceiver(
            Capsule capsule,
            Receiver receiver
    );
}
