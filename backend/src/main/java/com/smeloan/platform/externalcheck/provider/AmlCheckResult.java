package com.smeloan.platform.externalcheck.provider;

import com.smeloan.platform.externalcheck.entity.RiskLevel;
import java.util.List;

/**
 * Result of an AML/PEP screening check.
 *
 * @param riskLevel    assessed risk level for the screened party
 * @param pepMatch     {@code true} if the party is on a Politically Exposed Person list
 * @param matchedLists names of sanction/watch lists on which the party appeared
 * @param rawResponse  raw JSON string returned by the provider
 */
public record AmlCheckResult(
    RiskLevel riskLevel,
    boolean pepMatch,
    List<String> matchedLists,
    String rawResponse
) {}
