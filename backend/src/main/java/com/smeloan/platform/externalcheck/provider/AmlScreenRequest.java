package com.smeloan.platform.externalcheck.provider;

/**
 * Request parameters for an AML/PEP screening check.
 *
 * @param name     full name of the party to screen
 * @param idNumber government-issued ID number
 * @param ubn      company Unified Business Number
 */
public record AmlScreenRequest(
    String name,
    String idNumber,
    String ubn
) {}
