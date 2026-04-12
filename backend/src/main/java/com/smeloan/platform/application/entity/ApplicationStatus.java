package com.smeloan.platform.application.entity;

/**
 * All possible lifecycle states for a SME loan application.
 *
 * <pre>
 * DRAFT → PENDING_CUSTOMER → READY_FOR_CHECKS → CHECKING → CHECKS_COMPLETED
 *       → SCORING → SCORED → AUTO_APPROVED / AUTO_REJECTED / PENDING_REVIEW
 *       → APPROVED / REJECTED → OFFER_GENERATED → DISBURSED
 * </pre>
 */
public enum ApplicationStatus {

    /** Application created by RM but not yet shared with the customer. */
    DRAFT,

    /** Customer onboarding link has been sent; awaiting customer submission. */
    PENDING_CUSTOMER,

    /** The customer onboarding link has expired without submission. */
    EXPIRED,

    /** Customer has submitted all required information; ready for external checks. */
    READY_FOR_CHECKS,

    /** External checks (JCIC, AML, ETC) are currently running. */
    CHECKING,

    /** One or more external checks returned a failure result. */
    CHECK_FAILED,

    /** All external checks completed successfully. */
    CHECKS_COMPLETED,

    /** Credit scoring is in progress. */
    SCORING,

    /** Scoring has finished; awaiting a decision. */
    SCORED,

    /** Score is high enough for automatic approval. */
    AUTO_APPROVED,

    /** Score is too low; application is automatically rejected. */
    AUTO_REJECTED,

    /** Score falls in the manual review band; pending manager action. */
    PENDING_REVIEW,

    /** Manager has approved the application. */
    APPROVED,

    /** Manager has rejected the application. */
    REJECTED,

    /** Loan offer has been generated and sent to the customer. */
    OFFER_GENERATED,

    /** Loan has been disbursed to the customer. */
    DISBURSED
}
