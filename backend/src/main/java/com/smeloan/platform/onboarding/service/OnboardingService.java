package com.smeloan.platform.onboarding.service;

import com.smeloan.platform.application.dto.ApplicationDetailResponse;
import com.smeloan.platform.application.entity.DocumentType;
import com.smeloan.platform.onboarding.dto.OnboardingStatusResponse;
import com.smeloan.platform.onboarding.dto.SubmitConsentRequest;
import com.smeloan.platform.onboarding.dto.SubmitPartiesRequest;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service interface for the customer self-service onboarding flow.
 */
public interface OnboardingService {

    /**
     * Returns the validity and summary information for the given onboarding token.
     * Does not throw an exception if the token is invalid or expired — it simply
     * returns a response with {@code valid=false}.
     *
     * @param token the onboarding token from the URL
     * @return an {@link OnboardingStatusResponse} describing token validity
     */
    OnboardingStatusResponse getTokenStatus(String token);

    /**
     * Returns the full application detail for the given token.
     *
     * @param token the onboarding token
     * @return full application detail response
     * @throws com.smeloan.platform.common.exception.BusinessException if the token is invalid or expired
     */
    ApplicationDetailResponse getApplicationByToken(String token);

    /**
     * Saves or replaces the party records for the application identified by the token.
     * All existing parties for the application are removed and replaced.
     *
     * @param token   the onboarding token
     * @param request the party submission payload
     * @throws com.smeloan.platform.common.exception.BusinessException if the token is invalid or expired
     */
    void submitParties(String token, SubmitPartiesRequest request);

    /**
     * Stores an uploaded document against the application identified by the token.
     * Saves the file to local storage and creates a {@code Document} record.
     *
     * @param token    the onboarding token
     * @param type     the document type
     * @param file     the uploaded multipart file
     * @throws com.smeloan.platform.common.exception.BusinessException if the token is invalid or expired
     */
    void uploadDocument(String token, DocumentType type, MultipartFile file);

    /**
     * Records the customer's consent declarations. If all three consents are
     * {@code true} the application transitions to {@code READY_FOR_CHECKS} and
     * a {@link com.smeloan.platform.application.event.ReadyForChecksEvent} is published.
     *
     * @param token   the onboarding token
     * @param request the consent declarations
     * @throws com.smeloan.platform.common.exception.BusinessException if the token is invalid, expired,
     *         or if required consents are not granted
     */
    void submitConsent(String token, SubmitConsentRequest request);
}
