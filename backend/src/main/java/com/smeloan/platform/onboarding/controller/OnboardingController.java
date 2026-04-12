package com.smeloan.platform.onboarding.controller;

import com.smeloan.platform.application.dto.ApplicationDetailResponse;
import com.smeloan.platform.application.entity.DocumentType;
import com.smeloan.platform.common.dto.ApiResponse;
import com.smeloan.platform.onboarding.dto.OnboardingStatusResponse;
import com.smeloan.platform.onboarding.dto.SubmitConsentRequest;
import com.smeloan.platform.onboarding.dto.SubmitPartiesRequest;
import com.smeloan.platform.onboarding.service.OnboardingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST controller for the customer self-service onboarding flow.
 * All endpoints are accessible without authentication (secured by token).
 */
@RestController
@RequestMapping("/api/onboarding/{token}")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;

    /**
     * Checks whether the given onboarding token is valid and not expired.
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<OnboardingStatusResponse>> getStatus(@PathVariable String token) {
        OnboardingStatusResponse status = onboardingService.getTokenStatus(token);
        return ResponseEntity.ok(ApiResponse.ok(status));
    }

    /**
     * Returns the full application detail for this onboarding session.
     */
    @GetMapping("/application")
    public ResponseEntity<ApiResponse<ApplicationDetailResponse>> getApplication(@PathVariable String token) {
        ApplicationDetailResponse detail = onboardingService.getApplicationByToken(token);
        return ResponseEntity.ok(ApiResponse.ok(detail));
    }

    /**
     * Submits (or replaces) the party information for the application.
     */
    @PutMapping("/parties")
    public ResponseEntity<ApiResponse<Void>> submitParties(
            @PathVariable String token,
            @Valid @RequestBody SubmitPartiesRequest request) {
        onboardingService.submitParties(token, request);
        return ResponseEntity.ok(ApiResponse.ok("Parties saved successfully", null));
    }

    /**
     * Uploads a document for the application.
     *
     * @param documentType the document type (form parameter)
     * @param file         the uploaded file (multipart)
     */
    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void>> uploadDocument(
            @PathVariable String token,
            @RequestParam("documentType") DocumentType documentType,
            @RequestParam("file") MultipartFile file) {
        onboardingService.uploadDocument(token, documentType, file);
        return ResponseEntity.ok(ApiResponse.ok("Document uploaded successfully", null));
    }

    /**
     * Submits the customer's consent declarations. All three consents must be
     * {@code true} for the application to proceed to external checks.
     */
    @PostMapping("/consent")
    public ResponseEntity<ApiResponse<Void>> submitConsent(
            @PathVariable String token,
            @RequestBody SubmitConsentRequest request) {
        onboardingService.submitConsent(token, request);
        return ResponseEntity.ok(ApiResponse.ok("Consent recorded; application is now ready for checks", null));
    }
}
