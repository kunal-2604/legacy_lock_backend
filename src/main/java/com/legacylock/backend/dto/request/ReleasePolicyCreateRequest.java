package com.legacylock.backend.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReleasePolicyCreateRequest {

    @NotNull(message = "Inactivity days is required")
    @Min(value = 1, message = "Inactivity days must be at least 1")
    @Max(value = 3650, message = "Inactivity days cannot exceed 3650")
    private Integer inactivityDays;

    @NotNull(message = "Grace days is required")
    @Min(value = 0, message = "Grace days cannot be negative")
    @Max(value = 365, message = "Grace days cannot exceed 365")
    private Integer graceDays;
}
