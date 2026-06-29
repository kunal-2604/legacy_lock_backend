package com.legacylock.backend.controller;

import com.legacylock.backend.dto.request.ReleasePolicyCreateRequest;
import com.legacylock.backend.dto.request.ReleasePolicyUpdateRequest;
import com.legacylock.backend.dto.response.ReleasePolicyResponse;
import com.legacylock.backend.service.ReleasePolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/capsules/{capsuleId}/release-policy")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
public class ReleasePolicyController {

    private final ReleasePolicyService releasePolicyService;

    @PostMapping
    public ResponseEntity<ReleasePolicyResponse> createPolicy(
            @PathVariable UUID capsuleId,
            @Valid @RequestBody ReleasePolicyCreateRequest request
    ) {
        ReleasePolicyResponse response =
                releasePolicyService.createPolicy(capsuleId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<ReleasePolicyResponse> getPolicy(
            @PathVariable UUID capsuleId
    ) {
        ReleasePolicyResponse response =
                releasePolicyService.getPolicy(capsuleId);

        return ResponseEntity.ok(response);
    }

    @PutMapping
    public ResponseEntity<ReleasePolicyResponse> updatePolicy(
            @PathVariable UUID capsuleId,
            @Valid @RequestBody ReleasePolicyUpdateRequest request
    ) {
        ReleasePolicyResponse response =
                releasePolicyService.updatePolicy(capsuleId, request);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/pause")
    public ResponseEntity<ReleasePolicyResponse> pausePolicy(
            @PathVariable UUID capsuleId
    ) {
        ReleasePolicyResponse response =
                releasePolicyService.pausePolicy(capsuleId);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/activate")
    public ResponseEntity<ReleasePolicyResponse> activatePolicy(
            @PathVariable UUID capsuleId
    ) {
        ReleasePolicyResponse response =
                releasePolicyService.activatePolicy(capsuleId);

        return ResponseEntity.ok(response);
    }
}
