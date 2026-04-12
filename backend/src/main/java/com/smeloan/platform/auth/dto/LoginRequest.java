package com.smeloan.platform.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for user login.
 *
 * @param username the user's login name
 * @param password the user's plaintext password (will be verified against stored hash)
 */
public record LoginRequest(
    @NotBlank(message = "Username is required")
    String username,

    @NotBlank(message = "Password is required")
    String password
) {}
