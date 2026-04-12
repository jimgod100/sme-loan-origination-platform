package com.smeloan.platform.externalcheck.provider;

import com.smeloan.platform.externalcheck.entity.CheckType;

/**
 * Contract for credit bureau (JCIC) providers.
 */
public interface CreditBureauClient {

    /**
     * Queries the credit bureau for the given request.
     *
     * @param request the credit check parameters
     * @return the credit check result
     */
    CreditCheckResult query(CreditCheckRequest request);

    /**
     * Returns the display name of this provider.
     */
    String getProviderName();

    /**
     * Returns the {@link CheckType} handled by this provider.
     */
    CheckType getCheckType();
}
