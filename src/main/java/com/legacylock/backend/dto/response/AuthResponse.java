package com.legacylock.backend.dto.response;

import com.legacylock.backend.enums.Role;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    private UUID userId;
    private String name;
    private String email;
    private Role role;
    private String token;
}
