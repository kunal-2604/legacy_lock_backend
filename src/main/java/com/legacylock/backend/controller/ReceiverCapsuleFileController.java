package com.legacylock.backend.controller;

import com.legacylock.backend.dto.response.CapsuleFileResponse;
import com.legacylock.backend.dto.response.ReceiverCapsuleFileDownloadResponse;
import com.legacylock.backend.service.ReceiverCapsuleFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/receiver/capsules/{capsuleId}/files")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RECEIVER')")
public class ReceiverCapsuleFileController {

    private final ReceiverCapsuleFileService receiverCapsuleFileService;

    @GetMapping
    public ResponseEntity<List<CapsuleFileResponse>> getFiles(
            @PathVariable UUID capsuleId
    ) {
        List<CapsuleFileResponse> response =
                receiverCapsuleFileService.getReleasedCapsuleFiles(capsuleId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{fileId}/download")
    public ResponseEntity<byte[]> downloadFile(
            @PathVariable UUID capsuleId,
            @PathVariable UUID fileId
    ) {
        ReceiverCapsuleFileDownloadResponse file =
                receiverCapsuleFileService.downloadReleasedCapsuleFile(
                        capsuleId,
                        fileId
                );

        String contentType = file.getContentType() != null
                ? file.getContentType()
                : MediaType.APPLICATION_OCTET_STREAM_VALUE;

        ContentDisposition contentDisposition = ContentDisposition
                .attachment()
                .filename(file.getOriginalFileName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .contentType(MediaType.parseMediaType(contentType))
                .body(file.getFileBytes());
    }
}
