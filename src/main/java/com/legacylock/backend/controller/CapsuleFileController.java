package com.legacylock.backend.controller;

import com.legacylock.backend.dto.response.CapsuleFileResponse;
import com.legacylock.backend.service.CapsuleFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/capsules/{capsuleId}/files")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
public class CapsuleFileController {

    private final CapsuleFileService capsuleFileService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CapsuleFileResponse> uploadFile(
            @PathVariable UUID capsuleId,
            @RequestParam("file") MultipartFile file
    ) {
        CapsuleFileResponse response = capsuleFileService.uploadFile(capsuleId, file);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<CapsuleFileResponse>> getFiles(
            @PathVariable UUID capsuleId
    ) {
        List<CapsuleFileResponse> response = capsuleFileService.getFiles(capsuleId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> deleteFile(
            @PathVariable UUID capsuleId,
            @PathVariable UUID fileId
    ) {
        capsuleFileService.deleteFile(capsuleId, fileId);
        return ResponseEntity.noContent().build();
    }
}
