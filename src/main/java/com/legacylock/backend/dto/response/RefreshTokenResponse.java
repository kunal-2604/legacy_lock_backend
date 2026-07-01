package com.legacylock.backend.dto.response;

import com.legacylock.backend.enums.Role;
import lombok.*;

import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshTokenResponse {

    private UUID userId;
    private String name;
    private String email;
    private Set<Role> roles;

    private String accessToken;
    private String refreshToken;
    private String tokenType;
}
