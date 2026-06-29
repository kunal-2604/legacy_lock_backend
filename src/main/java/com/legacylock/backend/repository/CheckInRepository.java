package com.legacylock.backend.repository;

import com.legacylock.backend.entity.CheckIn;
import com.legacylock.backend.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CheckInRepository extends JpaRepository<CheckIn, UUID> {

    List<CheckIn> findByOwnerOrderByCheckedInAtDesc(Users owner);

    Optional<CheckIn> findFirstByOwnerOrderByCheckedInAtDesc(Users owner);
}
