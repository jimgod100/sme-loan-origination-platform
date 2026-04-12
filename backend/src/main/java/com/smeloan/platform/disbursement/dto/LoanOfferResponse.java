package com.smeloan.platform.disbursement.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Response record for a loan offer.
 *
 * @param id              offer database identifier
 * @param applicationId   the associated loan application ID
 * @param approvedAmount  approved principal amount
 * @param interestRate    annual interest rate (decimal)
 * @param termMonths      loan term in months
 * @param monthlyPayment  calculated monthly instalment amount
 * @param totalInterest   total interest payable over the loan term
 * @param offerStatus     current offer status (PENDING, ACCEPTED, REJECTED, EXPIRED)
 * @param offeredAt       when the offer was generated
 * @param acceptedAt      when the customer accepted the offer (nullable)
 * @param createdAt       record creation timestamp
 */
public record LoanOfferResponse(
    Long id,
    Long applicationId,
    BigDecimal approvedAmount,
    BigDecimal interestRate,
    int termMonths,
    BigDecimal monthlyPayment,
    BigDecimal totalInterest,
    String offerStatus,
    OffsetDateTime offeredAt,
    OffsetDateTime acceptedAt,
    OffsetDateTime createdAt
) {}
