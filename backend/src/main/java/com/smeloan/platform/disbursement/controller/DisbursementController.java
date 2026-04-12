package com.smeloan.platform.disbursement.controller;

import com.smeloan.platform.common.dto.ApiResponse;
import com.smeloan.platform.disbursement.dto.GenerateOfferRequest;
import com.smeloan.platform.disbursement.dto.LoanOfferResponse;
import com.smeloan.platform.disbursement.dto.RepaymentScheduleResponse;
import com.smeloan.platform.disbursement.service.DisbursementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for disbursement, loan-offer, and repayment-schedule operations.
 */
@RestController
@RequestMapping("/api/applications/{applicationId}")
@RequiredArgsConstructor
public class DisbursementController {

    private final DisbursementService disbursementService;

    /**
     * Generates a loan offer for the given approved application.
     */
    @PostMapping("/offer")
    public ResponseEntity<ApiResponse<LoanOfferResponse>> generateOffer(
            @PathVariable Long applicationId,
            @Valid @RequestBody GenerateOfferRequest request) {
        LoanOfferResponse response = disbursementService.generateOffer(applicationId, request);
        return ResponseEntity.ok(ApiResponse.ok("Loan offer generated successfully", response));
    }

    /**
     * Returns the loan offer for the given application.
     */
    @GetMapping("/offer")
    public ResponseEntity<ApiResponse<LoanOfferResponse>> getOffer(@PathVariable Long applicationId) {
        LoanOfferResponse response = disbursementService.getOffer(applicationId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Records the customer's acceptance of the offer.
     */
    @PostMapping("/offer/accept")
    public ResponseEntity<ApiResponse<Void>> acceptOffer(@PathVariable Long applicationId) {
        disbursementService.acceptOffer(applicationId);
        return ResponseEntity.ok(ApiResponse.ok("Offer accepted", null));
    }

    /**
     * Returns the complete repayment schedule for the application's offer.
     */
    @GetMapping("/repayment-schedule")
    public ResponseEntity<ApiResponse<List<RepaymentScheduleResponse>>> getRepaymentSchedule(
            @PathVariable Long applicationId) {
        List<RepaymentScheduleResponse> schedule = disbursementService.getRepaymentSchedule(applicationId);
        return ResponseEntity.ok(ApiResponse.ok(schedule));
    }

    /**
     * Returns the loan contract as a PDF file download.
     */
    @GetMapping("/contract-pdf")
    public ResponseEntity<byte[]> getContractPdf(@PathVariable Long applicationId) {
        byte[] pdfBytes = disbursementService.generateContractPdf(applicationId);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"loan-contract-" + applicationId + ".pdf\"")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdfBytes);
    }
}
