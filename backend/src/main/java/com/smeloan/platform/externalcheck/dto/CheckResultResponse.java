package com.smeloan.platform.externalcheck.dto;

import java.time.OffsetDateTime;

/**
 * Response record for a single external check result.
 *
 * @param id           result database identifier
 * @param checkType    type of check (JCIC, AML, ETC, PEP)
 * @param provider     name of the external provider
 * @param status       check execution status (PENDING, SUCCESS, FAILED, TIMEOUT)
 * @param riskLevel    assessed risk level (LOW, MEDIUM, HIGH) — nullable
 * @param errorMessage error message if the check failed — nullable
 * @param checkedAt    timestamp when the check completed — nullable
 * @param createdAt    record creation timestamp
 */
public record CheckResultResponse(
    Long id,
    String checkType,
    String provider,
    String status,
    String riskLevel,
    String errorMessage,
    OffsetDateTime checkedAt,
    OffsetDateTime createdAt
) {}
