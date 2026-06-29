package com.legacylock.backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReceiverCreateRequest {

    @NotBlank(message = "Receiver name is required")
    private String name;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Receiver email is required")
    private String email;

    @Pattern(
            regexp = "^[0-9]{10}$",
            message = "Phone number must be 10 digits"
    )
    private String phone;
}
