package com.smeloan.platform.application.dto;

import java.time.OffsetDateTime;

/**
 * Response returned when a customer onboarding token is generated.
 *
 * @param token     the UUID token to be embedded in the onboarding URL
 * @param url       the full onboarding URL to share with the customer
 * @param expiresAt when the token (and thus the URL) expires
 */
public record GenerateTokenResponse(
    String token,
    String url,
    OffsetDateTime expiresAt
) {}
