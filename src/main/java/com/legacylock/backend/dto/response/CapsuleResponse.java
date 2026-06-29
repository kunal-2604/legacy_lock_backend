package com.legacylock.backend.dto.response;

import com.legacylock.backend.enums.CapsuleStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CapsuleResponse {

    private UUID id;
    private String title;
    private String description;
    private String content;
    private CapsuleStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
