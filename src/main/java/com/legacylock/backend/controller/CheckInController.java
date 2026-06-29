package com.legacylock.backend.controller;

import com.legacylock.backend.dto.request.CheckInRequest;
import com.legacylock.backend.dto.response.CheckInResponse;
import com.legacylock.backend.service.CheckInService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/check-ins")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
public class CheckInController {

    private final CheckInService checkInService;

    @PostMapping
    public ResponseEntity<CheckInResponse> createCheckIn(
            @Valid @RequestBody(required = false) CheckInRequest request
    ) {
        if (request == null) {
            request = new CheckInRequest();
        }

        CheckInResponse response = checkInService.createCheckIn(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<CheckInResponse>> getMyCheckIns() {
        List<CheckInResponse> response = checkInService.getMyCheckIns();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/latest")
    public ResponseEntity<CheckInResponse> getLatestCheckIn() {
        CheckInResponse response = checkInService.getLatestCheckIn();
        return ResponseEntity.ok(response);
    }
}
