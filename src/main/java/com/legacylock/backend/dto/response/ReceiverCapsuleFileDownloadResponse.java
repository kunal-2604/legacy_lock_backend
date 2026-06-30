package com.legacylock.backend.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceiverCapsuleFileDownloadResponse {

    private String originalFileName;
    private String contentType;
    private byte[] fileBytes;
}
