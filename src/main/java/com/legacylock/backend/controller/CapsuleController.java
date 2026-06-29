package com.legacylock.backend.controller;

import com.legacylock.backend.dto.request.CapsuleCreateRequest;
import com.legacylock.backend.dto.request.CapsuleUpdateRequest;
import com.legacylock.backend.dto.response.CapsuleResponse;
import com.legacylock.backend.service.CapsuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/capsules")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
public class CapsuleController {

    private final CapsuleService capsuleService;

    @PostMapping
    public ResponseEntity<CapsuleResponse> createCapsule(
            @Valid @RequestBody CapsuleCreateRequest request
    ) {
        CapsuleResponse response = capsuleService.createCapsule(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<CapsuleResponse>> getMyCapsules() {
        List<CapsuleResponse> response = capsuleService.getMyCapsules();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{capsuleId}")
    public ResponseEntity<CapsuleResponse> getCapsuleById(
            @PathVariable UUID capsuleId
    ) {
        CapsuleResponse response = capsuleService.getCapsuleById(capsuleId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{capsuleId}")
    public ResponseEntity<CapsuleResponse> updateCapsule(
            @PathVariable UUID capsuleId,
            @Valid @RequestBody CapsuleUpdateRequest request
    ) {
        CapsuleResponse response = capsuleService.updateCapsule(capsuleId, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{capsuleId}/activate")
    public ResponseEntity<CapsuleResponse> activateCapsule(
            @PathVariable UUID capsuleId
    ) {
        CapsuleResponse response = capsuleService.activateCapsule(capsuleId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{capsuleId}")
    public ResponseEntity<Void> deleteCapsule(
            @PathVariable UUID capsuleId
    ) {
        capsuleService.deleteCapsule(capsuleId);
        return ResponseEntity.noContent().build();
    }
}
