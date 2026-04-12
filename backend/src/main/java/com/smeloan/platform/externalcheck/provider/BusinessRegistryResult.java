package com.smeloan.platform.externalcheck.provider;

/**
 * Result of a business registry / enterprise tax compliance lookup.
 *
 * @param status      company registration status: ACTIVE, SUSPENDED, or DISSOLVED
 * @param companyName company name as registered
 * @param industry    industry category as registered
 * @param rawResponse raw JSON string returned by the provider
 */
public record BusinessRegistryResult(
    String status,
    String companyName,
    String industry,
    String rawResponse
) {}
