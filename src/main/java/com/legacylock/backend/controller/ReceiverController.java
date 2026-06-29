package com.legacylock.backend.controller;

import com.legacylock.backend.dto.request.ReceiverCreateRequest;
import com.legacylock.backend.dto.request.ReceiverUpdateRequest;
import com.legacylock.backend.dto.response.ReceiverResponse;
import com.legacylock.backend.service.ReceiverService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/receivers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
public class ReceiverController {

    private final ReceiverService receiverService;

    @PostMapping
    public ResponseEntity<ReceiverResponse> createReceiver(
            @Valid @RequestBody ReceiverCreateRequest request
    ) {
        ReceiverResponse response = receiverService.createReceiver(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ReceiverResponse>> getMyReceivers() {
        List<ReceiverResponse> response = receiverService.getMyReceivers();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{receiverId}")
    public ResponseEntity<ReceiverResponse> getReceiverById(
            @PathVariable UUID receiverId
    ) {
        ReceiverResponse response = receiverService.getReceiverById(receiverId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{receiverId}")
    public ResponseEntity<ReceiverResponse> updateReceiver(
            @PathVariable UUID receiverId,
            @Valid @RequestBody ReceiverUpdateRequest request
    ) {
        ReceiverResponse response = receiverService.updateReceiver(receiverId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{receiverId}")
    public ResponseEntity<Void> removeReceiver(
            @PathVariable UUID receiverId
    ) {
        receiverService.removeReceiver(receiverId);
        return ResponseEntity.noContent().build();
    }
}
