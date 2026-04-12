package com.smeloan.platform.externalcheck.provider;

/**
 * Request parameters for a JCIC credit bureau check.
 *
 * @param ubn         the company's Unified Business Number
 * @param idNumber    the ID number of the primary responsible person
 * @param companyName the legal company name
 */
public record CreditCheckRequest(
    String ubn,
    String idNumber,
    String companyName
) {}
