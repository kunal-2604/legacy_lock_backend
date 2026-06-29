package com.legacylock.backend.controller;

import com.legacylock.backend.dto.response.ReceiverCapsuleDetailResponse;
import com.legacylock.backend.dto.response.ReceiverCapsuleResponse;
import com.legacylock.backend.service.ReceiverAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/receiver")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RECEIVER')")
public class ReceiverAccessController {

    private final ReceiverAccessService receiverAccessService;

    @GetMapping("/capsules")
    public ResponseEntity<List<ReceiverCapsuleResponse>> getMyReleasedCapsules() {
        List<ReceiverCapsuleResponse> response =
                receiverAccessService.getMyReleasedCapsules();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/capsules/{capsuleId}")
    public ResponseEntity<ReceiverCapsuleDetailResponse> getReleasedCapsuleById(
            @PathVariable UUID capsuleId
    ) {
        ReceiverCapsuleDetailResponse response =
                receiverAccessService.getReleasedCapsuleById(capsuleId);

        return ResponseEntity.ok(response);
    }
}
