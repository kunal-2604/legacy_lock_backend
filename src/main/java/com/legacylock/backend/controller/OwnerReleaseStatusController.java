package com.legacylock.backend.controller;

import com.legacylock.backend.dto.response.OwnerCapsuleReleaseStatusResponse;
import com.legacylock.backend.service.OwnerReleaseStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/owner/capsules")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
public class OwnerReleaseStatusController {

    private final OwnerReleaseStatusService ownerReleaseStatusService;

    @GetMapping("/{capsuleId}/release-status")
    public ResponseEntity<OwnerCapsuleReleaseStatusResponse> getCapsuleReleaseStatus(
            @PathVariable UUID capsuleId
    ) {
        OwnerCapsuleReleaseStatusResponse response =
                ownerReleaseStatusService.getCapsuleReleaseStatus(capsuleId);

        return ResponseEntity.ok(response);
    }
}
