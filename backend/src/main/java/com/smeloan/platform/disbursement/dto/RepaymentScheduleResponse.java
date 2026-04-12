package com.smeloan.platform.disbursement.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Response record for a single repayment schedule instalment.
 *
 * @param installmentNumber sequential instalment number (1-based)
 * @param dueDate           scheduled payment date
 * @param principal         principal portion of this payment
 * @param interest          interest portion of this payment
 * @param payment           total payment amount (principal + interest)
 * @param balance           outstanding balance after this payment
 * @param status            payment status (PENDING, PAID, OVERDUE)
 */
public record RepaymentScheduleResponse(
    int installmentNumber,
    LocalDate dueDate,
    BigDecimal principal,
    BigDecimal interest,
    BigDecimal payment,
    BigDecimal balance,
    String status
) {}
