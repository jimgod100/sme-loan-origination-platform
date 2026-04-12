package com.smeloan.platform.onboarding.dto;

import java.time.OffsetDateTime;

/**
 * Response showing the status of a customer onboarding token.
 *
 * @param valid             {@code true} if the token exists and has not expired
 * @param applicationNumber formatted application number associated with the token
 * @param companyName       name of the applying company
 * @param status            current application status
 * @param expiresAt         token expiry timestamp
 */
public record OnboardingStatusResponse(
    boolean valid,
    String applicationNumber,
    String companyName,
    String status,
    OffsetDateTime expiresAt
) {}
