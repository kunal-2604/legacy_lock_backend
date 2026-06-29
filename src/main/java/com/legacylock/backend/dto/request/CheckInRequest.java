package com.legacylock.backend.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckInRequest {

    @Size(max = 500, message = "Note cannot exceed 500 characters")
    private String note;
}
