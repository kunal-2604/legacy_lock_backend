package com.legacylock.backend.controller;

import com.legacylock.backend.dto.response.CapsuleReceiverResponse;
import com.legacylock.backend.service.CapsuleReceiverService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/capsules/{capsuleId}/receivers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
public class CapsuleReceiverController {

    private final CapsuleReceiverService capsuleReceiverService;

    @PostMapping("/{receiverId}")
    public ResponseEntity<CapsuleReceiverResponse> assignReceiverToCapsule(
            @PathVariable UUID capsuleId,
            @PathVariable UUID receiverId
    ) {
        CapsuleReceiverResponse response =
                capsuleReceiverService.assignReceiverToCapsule(capsuleId, receiverId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<CapsuleReceiverResponse>> getAssignedReceivers(
            @PathVariable UUID capsuleId
    ) {
        List<CapsuleReceiverResponse> response =
                capsuleReceiverService.getAssignedReceivers(capsuleId);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{receiverId}")
    public ResponseEntity<Void> removeReceiverFromCapsule(
            @PathVariable UUID capsuleId,
            @PathVariable UUID receiverId
    ) {
        capsuleReceiverService.removeReceiverFromCapsule(capsuleId, receiverId);
        return ResponseEntity.noContent().build();
    }
}
