package com.legacylock.backend.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckInResponse {

    private UUID id;
    private UUID ownerId;
    private String ownerEmail;
    private String note;
    private LocalDateTime checkedInAt;
}
