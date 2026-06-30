package com.legacylock.backend.repository;

import com.legacylock.backend.entity.Capsule;
import com.legacylock.backend.entity.CapsuleFile;
import com.legacylock.backend.enums.CapsuleFileStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CapsuleFileRepository extends JpaRepository<CapsuleFile, UUID> {

    List<CapsuleFile> findByCapsuleAndStatusOrderByUploadedAtDesc(
            Capsule capsule,
            CapsuleFileStatus status
    );

    Optional<CapsuleFile> findByIdAndCapsuleAndStatus(
            UUID id,
            Capsule capsule,
            CapsuleFileStatus status
    );
}
