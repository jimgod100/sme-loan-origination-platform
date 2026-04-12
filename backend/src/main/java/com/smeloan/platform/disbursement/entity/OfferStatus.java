package com.smeloan.platform.disbursement.entity;

/**
 * Lifecycle states of a loan offer.
 */
public enum OfferStatus {

    /** Offer has been generated but not yet actioned by the customer. */
    PENDING,

    /** Customer has formally accepted the offer. */
    ACCEPTED,

    /** Customer has declined the offer. */
    REJECTED,

    /** Offer validity period has elapsed without a customer response. */
    EXPIRED
}
