package com.legacylock.backend.controller;

import com.legacylock.backend.dto.request.AcknowledgementRequest;
import com.legacylock.backend.dto.response.AcknowledgementResponse;
import com.legacylock.backend.service.ReceiverAcknowledgementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/receiver/access-grants/{accessGrantId}/acknowledgement")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RECEIVER')")
public class ReceiverAcknowledgementController {

    private final ReceiverAcknowledgementService acknowledgementService;

    @PostMapping
    public ResponseEntity<AcknowledgementResponse> acknowledgeAccessGrant(
            @PathVariable UUID accessGrantId,
            @Valid @RequestBody(required = false) AcknowledgementRequest request
    ) {
        if (request == null) {
            request = new AcknowledgementRequest();
        }

        AcknowledgementResponse response =
                acknowledgementService.acknowledgeAccessGrant(accessGrantId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<AcknowledgementResponse> getMyAcknowledgement(
            @PathVariable UUID accessGrantId
    ) {
        AcknowledgementResponse response =
                acknowledgementService.getMyAcknowledgement(accessGrantId);

        return ResponseEntity.ok(response);
    }
}
