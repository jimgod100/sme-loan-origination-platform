package com.smeloan.platform.application.event;

/**
 * Spring application event published when a loan application transitions to
 * {@link com.smeloan.platform.application.entity.ApplicationStatus#READY_FOR_CHECKS}.
 * Listeners (e.g. the external-check service) should react by starting the
 * asynchronous check pipeline.
 *
 * @param applicationId the ID of the loan application that is ready for checks
 */
public record ReadyForChecksEvent(Long applicationId) {}
