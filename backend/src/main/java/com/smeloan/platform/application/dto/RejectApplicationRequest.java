package com.smeloan.platform.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for rejecting a loan application.
 *
 * @param reason the mandatory rejection reason provided by the manager
 */
public record RejectApplicationRequest(
    @NotBlank(message = "Rejection reason is required")
    @Size(max = 1024, message = "Rejection reason must not exceed 1024 characters")
    String reason
) {}
