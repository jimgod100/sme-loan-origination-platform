package com.smeloan.platform.application.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request payload for creating a new loan application.
 *
 * @param ubn                    8-digit Unified Business Number of the applicant company
 * @param companyName            legal name of the company
 * @param industry               industry sector
 * @param establishedDate        date the company was incorporated
 * @param capital                registered capital amount
 * @param address                company registered address
 * @param contactPhone           primary contact phone number
 * @param contactEmail           primary contact email address
 * @param requestedAmount        requested loan amount (must be positive)
 * @param requestedTermMonths    requested loan term in months (1-360)
 * @param loanPurpose            purpose/description for the loan
 * @param collateralType         type of collateral offered
 * @param collateralDescription  detailed description of the collateral
 */
public record CreateApplicationRequest(

    @NotBlank(message = "UBN is required")
    @Size(min = 8, max = 8, message = "UBN must be exactly 8 characters")
    @Pattern(regexp = "\\d{8}", message = "UBN must consist of 8 digits")
    String ubn,

    @NotBlank(message = "Company name is required")
    @Size(max = 256, message = "Company name must not exceed 256 characters")
    String companyName,

    @Size(max = 128, message = "Industry must not exceed 128 characters")
    String industry,

    LocalDate establishedDate,

    @DecimalMin(value = "0.0", inclusive = false, message = "Capital must be positive")
    BigDecimal capital,

    @Size(max = 512, message = "Address must not exceed 512 characters")
    String address,

    @Size(max = 32, message = "Contact phone must not exceed 32 characters")
    String contactPhone,

    @Email(message = "Contact email must be a valid email address")
    @Size(max = 128, message = "Contact email must not exceed 128 characters")
    String contactEmail,

    @NotNull(message = "Requested amount is required")
    @DecimalMin(value = "1.00", message = "Requested amount must be at least 1")
    BigDecimal requestedAmount,

    @Min(value = 1, message = "Term must be at least 1 month")
    @Max(value = 360, message = "Term must not exceed 360 months")
    int requestedTermMonths,

    @Size(max = 512, message = "Loan purpose must not exceed 512 characters")
    String loanPurpose,

    @Size(max = 128, message = "Collateral type must not exceed 128 characters")
    String collateralType,

    @Size(max = 1024, message = "Collateral description must not exceed 1024 characters")
    String collateralDescription
) {}
