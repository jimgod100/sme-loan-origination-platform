package com.smeloan.platform.externalcheck.provider;

/**
 * Result of a JCIC credit bureau check.
 *
 * @param creditScore      credit score in the range 400–900
 * @param delinquencyCount number of past delinquency events
 * @param debtRatio        total debt / total assets ratio (0.0 – 1.0+)
 * @param rawResponse      raw JSON string returned by the provider
 */
public record CreditCheckResult(
    int creditScore,
    int delinquencyCount,
    double debtRatio,
    String rawResponse
) {}
