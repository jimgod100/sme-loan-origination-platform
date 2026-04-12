package com.smeloan.platform.application.entity;

/**
 * Categories of documents that may be uploaded as part of a loan application.
 */
public enum DocumentType {

    /** Annual or interim financial statements of the company. */
    FINANCIAL_STATEMENT,

    /** Bank account transaction history. */
    BANK_STATEMENT,

    /** National ID card of a responsible person or guarantor. */
    ID_CARD,

    /** Passport of a responsible person or guarantor. */
    PASSPORT,

    /** Business or collateral-related contracts. */
    CONTRACT
}
