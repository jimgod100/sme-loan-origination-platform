package com.smeloan.platform.externalcheck.entity;

/**
 * Execution status of an individual external check.
 */
public enum CheckStatus {

    /** The check has been initiated but has not yet received a response. */
    PENDING,

    /** The check completed and returned a valid response. */
    SUCCESS,

    /** The check completed but the provider returned an error or rejection. */
    FAILED,

    /** The check did not receive a response within the allowed time window. */
    TIMEOUT
}
