package com.smeloan.platform.disbursement.service;

import com.smeloan.platform.disbursement.dto.GenerateOfferRequest;
import com.smeloan.platform.disbursement.dto.LoanOfferResponse;
import com.smeloan.platform.disbursement.dto.RepaymentScheduleResponse;

import java.util.List;

/**
 * Service interface for disbursement and loan-offer operations.
 */
public interface DisbursementService {

    /**
     * Generates a loan offer for an approved application, computing the monthly
     * payment via the standard amortisation formula and creating the full
     * repayment schedule.
     * Transitions the application status to {@code OFFER_GENERATED}.
     *
     * @param applicationId the ID of the approved loan application
     * @param request       offer parameters (amount, rate, term)
     * @return the generated loan offer response
     */
    LoanOfferResponse generateOffer(Long applicationId, GenerateOfferRequest request);

    /**
     * Retrieves the loan offer for the given application.
     *
     * @param applicationId the ID of the loan application
     * @return the loan offer response
     * @throws com.smeloan.platform.common.exception.ResourceNotFoundException if no offer exists
     */
    LoanOfferResponse getOffer(Long applicationId);

    /**
     * Records the customer's acceptance of the loan offer.
     * Updates offer status to {@code ACCEPTED}.
     *
     * @param applicationId the ID of the loan application
     */
    void acceptOffer(Long applicationId);

    /**
     * Returns the complete repayment schedule for the given application's offer.
     *
     * @param applicationId the ID of the loan application
     * @return an ordered list of instalment responses
     */
    List<RepaymentScheduleResponse> getRepaymentSchedule(Long applicationId);

    /**
     * Generates the loan contract as a PDF byte array.
     * TODO: Integrate a PDF generation library (e.g. iText, JasperReports).
     *
     * @param applicationId the ID of the loan application
     * @return the PDF bytes
     */
    byte[] generateContractPdf(Long applicationId);
}
