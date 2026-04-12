package com.smeloan.platform.onboarding.dto;

/**
 * Request payload for the customer to submit their consent declarations.
 *
 * @param consentJcic            consent for the platform to query JCIC credit bureau
 * @param consentDataCollection  consent for personal data collection and processing
 * @param consentAml             consent for AML / PEP screening
 */
public record SubmitConsentRequest(
    boolean consentJcic,
    boolean consentDataCollection,
    boolean consentAml
) {}
