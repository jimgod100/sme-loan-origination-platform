package com.smeloan.platform.externalcheck.controller;

import com.smeloan.platform.common.dto.ApiResponse;
import com.smeloan.platform.externalcheck.dto.CheckResultResponse;
import com.smeloan.platform.externalcheck.entity.CheckType;
import com.smeloan.platform.externalcheck.service.ExternalCheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing external check results on a loan application.
 */
@RestController
@RequestMapping("/api/applications/{applicationId}/checks")
@RequiredArgsConstructor
public class ExternalCheckController {

    private final ExternalCheckService externalCheckService;

    /**
     * Returns all external check results for the given application.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<CheckResultResponse>>> getCheckResults(
            @PathVariable Long applicationId) {
        List<CheckResultResponse> results = externalCheckService.getCheckResults(applicationId);
        return ResponseEntity.ok(ApiResponse.ok(results));
    }

    /**
     * Retries a specific failed or timed-out external check.
     *
     * @param checkType the type of check to retry (JCIC, AML, ETC, PEP)
     */
    @PostMapping("/retry/{checkType}")
    public ResponseEntity<ApiResponse<Void>> retryCheck(
            @PathVariable Long applicationId,
            @PathVariable CheckType checkType) {
        externalCheckService.retryCheck(applicationId, checkType);
        return ResponseEntity.ok(ApiResponse.ok("Check retry initiated for " + checkType, null));
    }
}
