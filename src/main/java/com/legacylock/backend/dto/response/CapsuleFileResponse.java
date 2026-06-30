package com.legacylock.backend.dto.response;

import com.legacylock.backend.enums.CapsuleFileStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CapsuleFileResponse {

    private UUID id;
    private UUID capsuleId;

    private String originalFileName;
    private String storedFileKey;
    private String contentType;
    private Long fileSize;
    private String checksum;

    private CapsuleFileStatus status;
    private LocalDateTime uploadedAt;
}
