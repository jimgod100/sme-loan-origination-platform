package com.smeloan.platform.scoring.dto;

import jakarta.validation.constraints.*;

/**
 * Request payload for simulating the credit score without a real application.
 *
 * @param jcicScore            credit bureau score (400–900)
 * @param debtRatio            total debt / total assets ratio (0.0–2.0)
 * @param interestCoverage     EBIT / interest expense ratio (0.0+)
 * @param companyAgeYears      years since company incorporation
 * @param amlRiskLevel         AML risk level string: LOW, MEDIUM, or HIGH
 */
public record SimulateScoreRequest(
    @Min(400) @Max(900)
    int jcicScore,

    @DecimalMin("0.0") @DecimalMax("2.0")
    double debtRatio,

    @DecimalMin("0.0")
    double interestCoverage,

    @Min(0)
    int companyAgeYears,

    @NotBlank
    @Pattern(regexp = "LOW|MEDIUM|HIGH", message = "amlRiskLevel must be LOW, MEDIUM or HIGH")
    String amlRiskLevel
) {}
