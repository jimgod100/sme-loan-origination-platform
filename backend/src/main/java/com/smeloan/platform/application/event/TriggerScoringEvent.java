package com.smeloan.platform.application.event;

/**
 * Spring application event published when all external checks have completed
 * and the application is ready to be scored.
 *
 * @param applicationId the ID of the loan application to be scored
 */
public record TriggerScoringEvent(Long applicationId) {}
