package com.smeloan.platform.application.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Summary response for a loan application (used in create/update responses).
 *
 * @param id                   application database identifier
 * @param applicationNumber    unique formatted application number
 * @param customerName         name of the applicant company
 * @param rmName               full name of the Relationship Manager
 * @param status               current application status
 * @param requestedAmount      requested loan amount
 * @param requestedTermMonths  requested loan term in months
 * @param loanPurpose          purpose of the loan
 * @param collateralType       type of collateral offered
 * @param partyCount           number of associated parties
 * @param documentCount        number of uploaded documents
 * @param customerToken        onboarding token (nullable if not yet generated)
 * @param tokenExpiresAt       expiry of the onboarding token
 * @param createdAt            application creation timestamp
 * @param updatedAt            last update timestamp
 */
public record ApplicationResponse(
    Long id,
    String applicationNumber,
    String customerName,
    String rmName,
    String status,
    BigDecimal requestedAmount,
    int requestedTermMonths,
    String loanPurpose,
    String collateralType,
    int partyCount,
    int documentCount,
    String customerToken,
    OffsetDateTime tokenExpiresAt,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
