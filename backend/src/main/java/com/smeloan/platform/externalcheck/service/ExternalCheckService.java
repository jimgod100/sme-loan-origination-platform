package com.smeloan.platform.externalcheck.service;

import com.smeloan.platform.externalcheck.dto.CheckResultResponse;
import com.smeloan.platform.externalcheck.entity.CheckType;

import java.util.List;

/**
 * Service interface for running and retrieving external underwriting checks.
 */
public interface ExternalCheckService {

    /**
     * Asynchronously executes all configured external checks (JCIC, AML, ETC)
     * for the given loan application. Creates PENDING records first, then
     * executes each check concurrently. When all checks complete successfully
     * the application status transitions to {@code CHECKS_COMPLETED}; if any
     * check fails the status transitions to {@code CHECK_FAILED}.
     *
     * @param applicationId the ID of the loan application to check
     */
    void executeAllChecks(Long applicationId);

    /**
     * Retries a single failed or timed-out check for the given application.
     *
     * @param applicationId the ID of the loan application
     * @param checkType     the type of check to retry
     */
    void retryCheck(Long applicationId, CheckType checkType);

    /**
     * Returns all external check results for the given application.
     *
     * @param applicationId the ID of the loan application
     * @return list of check result responses
     */
    List<CheckResultResponse> getCheckResults(Long applicationId);
}
