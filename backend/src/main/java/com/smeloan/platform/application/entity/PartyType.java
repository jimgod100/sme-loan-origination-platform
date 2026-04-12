package com.smeloan.platform.application.entity;

/**
 * Classifies the role of a party associated with a loan application.
 */
public enum PartyType {

    /** Legal representative or director of the applying company. */
    RESPONSIBLE_PERSON,

    /** An individual or entity providing a guarantee for the loan. */
    GUARANTOR,

    /** A contact person (e.g. accountant, legal counsel) for the application. */
    CONTACT
}
