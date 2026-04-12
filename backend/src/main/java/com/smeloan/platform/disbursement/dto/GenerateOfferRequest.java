package com.smeloan.platform.disbursement.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * Request payload for generating a loan offer.
 *
 * @param approvedAmount  the principal amount approved for disbursement
 * @param interestRate    annual interest rate as a decimal (e.g. 0.045 = 4.5%)
 * @param termMonths      loan duration in months (1–360)
 */
public record GenerateOfferRequest(

    @NotNull(message = "Approved amount is required")
    @DecimalMin(value = "1.00", message = "Approved amount must be at least 1")
    BigDecimal approvedAmount,

    @NotNull(message = "Interest rate is required")
    @DecimalMin(value = "0.0001", message = "Interest rate must be positive")
    @DecimalMax(value = "1.0000", message = "Interest rate must not exceed 100%")
    BigDecimal interestRate,

    @Min(value = 1, message = "Term must be at least 1 month")
    @Max(value = 360, message = "Term must not exceed 360 months")
    int termMonths
) {}
