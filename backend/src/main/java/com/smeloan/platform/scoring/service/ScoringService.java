package com.smeloan.platform.scoring.service;

import com.smeloan.platform.scoring.dto.ScoreResultResponse;
import com.smeloan.platform.scoring.dto.SimulateScoreRequest;
import com.smeloan.platform.scoring.dto.SimulateScoreResponse;

/**
 * Service interface for credit scoring operations.
 */
public interface ScoringService {

    /**
     * Scores the given loan application using the configured scoring model.
     * External check results must already be available.
     * Updates the application status based on the scoring decision.
     *
     * @param applicationId the ID of the application to score
     * @return the scoring result response
     * @throws com.smeloan.platform.common.exception.ResourceNotFoundException if not found
     */
    ScoreResultResponse score(Long applicationId);

    /**
     * Runs a hypothetical scoring simulation without persisting any data.
     * Useful for RM training or what-if analysis.
     *
     * @param request the input parameters for simulation
     * @return the simulated score, decision, and breakdown
     */
    SimulateScoreResponse simulate(SimulateScoreRequest request);

    /**
     * Returns the persisted scoring result for the given application.
     *
     * @param applicationId the ID of the application
     * @return the scoring result response
     * @throws com.smeloan.platform.common.exception.ResourceNotFoundException if not found
     */
    ScoreResultResponse getResult(Long applicationId);
}
