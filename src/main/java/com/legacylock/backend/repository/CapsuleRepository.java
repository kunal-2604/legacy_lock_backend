package com.legacylock.backend.repository;

import com.legacylock.backend.entity.Capsule;
import com.legacylock.backend.entity.Users;
import com.legacylock.backend.enums.CapsuleStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CapsuleRepository extends JpaRepository<Capsule, UUID> {

    List<Capsule> findByOwnerAndStatusNotOrderByCreatedAtDesc(
            Users owner,
            CapsuleStatus status
    );

    Optional<Capsule> findByIdAndOwner(UUID id, Users owner);

    boolean existsByOwnerAndTitleAndStatusNot(
            Users owner,
            String title,
            CapsuleStatus status
    );
}
