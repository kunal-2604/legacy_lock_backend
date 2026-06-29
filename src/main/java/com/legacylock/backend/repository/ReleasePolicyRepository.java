package com.legacylock.backend.repository;

import com.legacylock.backend.entity.Capsule;
import com.legacylock.backend.entity.ReleasePolicy;
import com.legacylock.backend.enums.ReleasePolicyStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReleasePolicyRepository extends JpaRepository<ReleasePolicy, UUID> {

    Optional<ReleasePolicy> findByCapsule(Capsule capsule);

    boolean existsByCapsule(Capsule capsule);

    List<ReleasePolicy> findByStatus(ReleasePolicyStatus status);
}
