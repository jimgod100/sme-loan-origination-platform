package com.smeloan.platform.externalcheck.provider;

/**
 * Contract for business registry / enterprise tax compliance providers.
 */
public interface BusinessRegistryClient {

    /**
     * Looks up the business registration record for the given UBN.
     *
     * @param ubn the 8-digit Unified Business Number
     * @return the business registry result
     */
    BusinessRegistryResult lookup(String ubn);

    /**
     * Returns the display name of this provider.
     */
    String getProviderName();
}
