package com.smeloan.platform.disbursement.entity;

/**
 * Payment status of a single repayment schedule instalment.
 */
public enum RepaymentStatus {

    /** Instalment is scheduled but not yet due or paid. */
    PENDING,

    /** Instalment has been fully paid. */
    PAID,

    /** Instalment is past its due date and has not been paid. */
    OVERDUE
}
