package com.smeloan.platform.application.controller;

import com.smeloan.platform.application.dto.*;
import com.smeloan.platform.application.entity.ApplicationStatus;
import com.smeloan.platform.application.service.LoanApplicationService;
import com.smeloan.platform.common.dto.ApiResponse;
import com.smeloan.platform.common.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for loan application lifecycle management.
 */
@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class LoanApplicationController {

    private final LoanApplicationService loanApplicationService;

    /**
     * Creates a new loan application.
     * The authenticated user is used as the Relationship Manager.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ApplicationResponse>> createApplication(
            @Valid @RequestBody CreateApplicationRequest request,
            Authentication authentication) {
        // In a real implementation extract rmUserId from JWT claims
        // For now use a placeholder ID resolved from auth principal attribute
        Long rmUserId = extractUserId(authentication);
        ApplicationResponse response = loanApplicationService.createApplication(request, rmUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Application created successfully", response));
    }

    /**
     * Lists all applications with optional status filter and pagination.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ApplicationListResponse>>> listApplications(
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("ASC")
            ? Sort.by(sortBy).ascending()
            : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ApplicationListResponse> resultPage = loanApplicationService.listApplications(status, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(resultPage)));
    }

    /**
     * Returns the full detail of a single application.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ApplicationDetailResponse>> getApplication(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(loanApplicationService.getApplication(id)));
    }

    /**
     * Generates a customer onboarding token and transitions status to PENDING_CUSTOMER.
     */
    @PostMapping("/{id}/generate-token")
    public ResponseEntity<ApiResponse<GenerateTokenResponse>> generateToken(@PathVariable Long id) {
        return ResponseEntity.ok(
            ApiResponse.ok("Token generated successfully", loanApplicationService.generateCustomerToken(id)));
    }

    /**
     * Manually triggers the external check pipeline.
     */
    @PostMapping("/{id}/trigger-checks")
    public ResponseEntity<ApiResponse<Void>> triggerChecks(@PathVariable Long id) {
        loanApplicationService.triggerExternalChecks(id);
        return ResponseEntity.ok(ApiResponse.ok("External checks triggered", null));
    }

    /**
     * Manually triggers the scoring pipeline.
     */
    @PostMapping("/{id}/score")
    public ResponseEntity<ApiResponse<Void>> triggerScoring(@PathVariable Long id) {
        loanApplicationService.triggerScoring(id);
        return ResponseEntity.ok(ApiResponse.ok("Scoring triggered", null));
    }

    /**
     * Approves the application.
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<Void>> approve(
            @PathVariable Long id,
            Authentication authentication) {
        Long managerId = extractUserId(authentication);
        loanApplicationService.approveApplication(id, managerId);
        return ResponseEntity.ok(ApiResponse.ok("Application approved", null));
    }

    /**
     * Rejects the application with a mandatory reason.
     */
    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<Void>> reject(
            @PathVariable Long id,
            @Valid @RequestBody RejectApplicationRequest request,
            Authentication authentication) {
        Long managerId = extractUserId(authentication);
        loanApplicationService.rejectApplication(id, managerId, request.reason());
        return ResponseEntity.ok(ApiResponse.ok("Application rejected", null));
    }

    /**
     * Marks the loan as disbursed.
     */
    @PostMapping("/{id}/disburse")
    public ResponseEntity<ApiResponse<Void>> disburse(@PathVariable Long id) {
        loanApplicationService.disburseApplication(id);
        return ResponseEntity.ok(ApiResponse.ok("Application marked as disbursed", null));
    }

    // ------------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------------

    /**
     * Extracts the user ID from the Spring Security {@link Authentication}.
     * TODO: Replace with proper JWT claims extraction once JWT is wired up.
     */
    private Long extractUserId(Authentication authentication) {
        if (authentication != null && authentication.getDetails() instanceof Long) {
            return (Long) authentication.getDetails();
        }
        // Fallback: resolve from principal name in UserRepository if needed
        // For now return 1L as a development stub
        return 1L;
    }
}
