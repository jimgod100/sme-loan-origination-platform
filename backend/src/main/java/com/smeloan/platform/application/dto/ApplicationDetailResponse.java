package com.smeloan.platform.application.dto;

import com.smeloan.platform.externalcheck.dto.CheckResultResponse;
import com.smeloan.platform.scoring.dto.ScoreResultResponse;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Full detail response for a loan application, including nested objects.
 *
 * @param id                   application database identifier
 * @param applicationNumber    unique formatted application number
 * @param status               current application status
 * @param requestedAmount      requested loan amount
 * @param requestedTermMonths  requested loan term in months
 * @param loanPurpose          stated loan purpose
 * @param collateralType       collateral type
 * @param collateralDescription description of the collateral
 * @param customerToken        onboarding token
 * @param tokenExpiresAt       onboarding token expiry
 * @param rejectionReason      manager rejection reason (if applicable)
 * @param reviewedAt           date/time the application was reviewed
 * @param createdAt            creation timestamp
 * @param updatedAt            last update timestamp
 * @param customer             full customer details
 * @param rmName               Relationship Manager full name
 * @param reviewedBy           name of reviewing manager (if applicable)
 * @param parties              list of associated parties
 * @param documents            list of uploaded documents
 * @param checkResults         list of external check results
 * @param scoreResult          scoring result (nullable if not yet scored)
 */
public record ApplicationDetailResponse(
    Long id,
    String applicationNumber,
    String status,
    BigDecimal requestedAmount,
    int requestedTermMonths,
    String loanPurpose,
    String collateralType,
    String collateralDescription,
    String customerToken,
    OffsetDateTime tokenExpiresAt,
    String rejectionReason,
    OffsetDateTime reviewedAt,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    CustomerResponse customer,
    String rmName,
    String reviewedBy,
    List<PartyResponse> parties,
    List<DocumentResponse> documents,
    List<CheckResultResponse> checkResults,
    ScoreResultResponse scoreResult
) {}
