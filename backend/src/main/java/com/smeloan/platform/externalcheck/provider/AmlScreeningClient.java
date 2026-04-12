package com.smeloan.platform.externalcheck.provider;

/**
 * Contract for AML / PEP screening providers.
 */
public interface AmlScreeningClient {

    /**
     * Screens the given subject for AML and PEP risk.
     *
     * @param request the screening parameters
     * @return the screening result
     */
    AmlCheckResult screen(AmlScreenRequest request);

    /**
     * Returns the display name of this provider.
     */
    String getProviderName();
}
