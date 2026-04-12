package com.smeloan.platform.application.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Lightweight response record used in paginated list views.
 *
 * @param id                   application database identifier
 * @param applicationNumber    unique formatted application number
 * @param customerName         name of the applicant company
 * @param ubn                  Unified Business Number of the customer
 * @param rmName               full name of the Relationship Manager
 * @param status               current application status
 * @param requestedAmount      requested loan amount
 * @param requestedTermMonths  requested loan term in months
 * @param createdAt            application creation timestamp
 */
public record ApplicationListResponse(
    Long id,
    String applicationNumber,
    String customerName,
    String ubn,
    String rmName,
    String status,
    BigDecimal requestedAmount,
    int requestedTermMonths,
    OffsetDateTime createdAt
) {}
