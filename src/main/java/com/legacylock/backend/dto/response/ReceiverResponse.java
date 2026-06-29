package com.legacylock.backend.dto.response;

import com.legacylock.backend.enums.ReceiverStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceiverResponse {

    private UUID id;
    private String name;
    private String email;
    private String phone;
    private ReceiverStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
