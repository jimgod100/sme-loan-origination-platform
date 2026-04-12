package com.smeloan.platform.externalcheck.entity;

/**
 * Types of external checks performed during the loan underwriting process.
 */
public enum CheckType {

    /** Joint Credit Information Center — credit bureau check. */
    JCIC,

    /** Anti-Money Laundering screening. */
    AML,

    /** Enterprise Tax Compliance check via the business registry. */
    ETC,

    /** Politically Exposed Person screening. */
    PEP
}
