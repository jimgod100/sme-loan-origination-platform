package com.smeloan.platform.application.service;

import com.smeloan.platform.application.dto.*;
import com.smeloan.platform.application.entity.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for managing the full lifecycle of SME loan applications.
 */
public interface LoanApplicationService {

    /**
     * Creates a new loan application. If a customer with the given UBN already
     * exists, the existing record is reused; otherwise a new Customer is created.
     *
     * @param request  the application creation request
     * @param rmUserId the ID of the Relationship Manager creating the application
     * @return a summary response for the newly created application
     */
    ApplicationResponse createApplication(CreateApplicationRequest request, Long rmUserId);

    /**
     * Retrieves the full detail of a single loan application by its ID.
     *
     * @param id the application database identifier
     * @return the full application detail response
     * @throws com.smeloan.platform.common.exception.ResourceNotFoundException if not found
     */
    ApplicationDetailResponse getApplication(Long id);

    /**
     * Returns a paginated list of all applications, optionally filtered by status.
     *
     * @param status   optional status filter; pass {@code null} to return all statuses
     * @param pageable pagination and sorting parameters
     * @return a page of list-view application summaries
     */
    Page<ApplicationListResponse> listApplications(ApplicationStatus status, Pageable pageable);

    /**
     * Returns a paginated list of applications assigned to a specific RM.
     *
     * @param rmUserId the Relationship Manager's user ID
     * @param pageable pagination and sorting parameters
     * @return a page of list-view application summaries
     */
    Page<ApplicationListResponse> listApplicationsByRm(Long rmUserId, Pageable pageable);

    /**
     * Generates a customer onboarding token for the given application and
     * transitions the application to {@code PENDING_CUSTOMER} status.
     * The token is valid for 7 days.
     *
     * @param applicationId the application ID
     * @return the generated token and the full onboarding URL
     */
    GenerateTokenResponse generateCustomerToken(Long applicationId);

    /**
     * Manually triggers the external check pipeline for the given application.
     * The application must currently be in {@code READY_FOR_CHECKS} status.
     *
     * @param applicationId the application ID
     */
    void triggerExternalChecks(Long applicationId);

    /**
     * Manually triggers the credit-scoring pipeline.
     * The application must currently be in {@code CHECKS_COMPLETED} status.
     *
     * @param applicationId the application ID
     */
    void triggerScoring(Long applicationId);

    /**
     * Approves a loan application. The application must be in
     * {@code PENDING_REVIEW} or {@code SCORED} status.
     *
     * @param applicationId the application ID
     * @param managerId     the ID of the approving manager
     */
    void approveApplication(Long applicationId, Long managerId);

    /**
     * Rejects a loan application with a mandatory reason. The application must
     * be in {@code PENDING_REVIEW} or {@code SCORED} status.
     *
     * @param applicationId the application ID
     * @param managerId     the ID of the rejecting manager
     * @param reason        the reason for rejection
     */
    void rejectApplication(Long applicationId, Long managerId, String reason);

    /**
     * Marks the loan as disbursed. The application must be in
     * {@code APPROVED} or {@code OFFER_GENERATED} status.
     *
     * @param applicationId the application ID
     */
    void disburseApplication(Long applicationId);
}
