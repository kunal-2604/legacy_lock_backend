package com.legacylock.backend.service;

import com.legacylock.backend.dto.request.CheckInRequest;
import com.legacylock.backend.dto.response.CheckInResponse;
import com.legacylock.backend.entity.CheckIn;
import com.legacylock.backend.entity.Users;
import com.legacylock.backend.enums.AuditAction;
import com.legacylock.backend.enums.Role;
import com.legacylock.backend.exceptions.LegacyLockException;
import com.legacylock.backend.repository.CheckInRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CheckInService {

    private final CheckInRepository checkInRepository;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;

    public CheckInResponse createCheckIn(CheckInRequest request) {

        Users owner = currentUserService.getCurrentUser();

        validateOwnerRole(owner);

        CheckIn checkIn = CheckIn.builder()
                .owner(owner)
                .note(normalizeOptionalText(request.getNote()))
                .build();

        CheckIn savedCheckIn = checkInRepository.save(checkIn);

        auditLogService.log(
                owner,
                AuditAction.CHECK_IN_CREATED,
                "CHECK_IN",
                savedCheckIn.getId(),
                "Owner checked in"
        );

        return mapToResponse(savedCheckIn);
    }

    @Transactional(readOnly = true)
    public List<CheckInResponse> getMyCheckIns() {

        Users owner = currentUserService.getCurrentUser();

        validateOwnerRole(owner);

        return checkInRepository.findByOwnerOrderByCheckedInAtDesc(owner)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CheckInResponse getLatestCheckIn() {

        Users owner = currentUserService.getCurrentUser();

        validateOwnerRole(owner);

        CheckIn latestCheckIn = checkInRepository
                .findFirstByOwnerOrderByCheckedInAtDesc(owner)
                .orElseThrow(() -> new LegacyLockException("No check-in found"));

        return mapToResponse(latestCheckIn);
    }

    private void validateOwnerRole(Users user) {
        if (user.getRole() != Role.OWNER) {
            throw new LegacyLockException("Only owners can check in");
        }
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return value.trim();
    }

    private CheckInResponse mapToResponse(CheckIn checkIn) {
        return CheckInResponse.builder()
                .id(checkIn.getId())
                .ownerId(checkIn.getOwner().getId())
                .ownerEmail(checkIn.getOwner().getEmail())
                .note(checkIn.getNote())
                .checkedInAt(checkIn.getCheckedInAt())
                .build();
    }
}
