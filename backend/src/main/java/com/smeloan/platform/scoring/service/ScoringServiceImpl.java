package com.smeloan.platform.scoring.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smeloan.platform.common.exception.ResourceNotFoundException;
import com.smeloan.platform.scoring.dto.ScoreResultResponse;
import com.smeloan.platform.scoring.dto.SimulateScoreRequest;
import com.smeloan.platform.scoring.dto.SimulateScoreResponse;
import com.smeloan.platform.scoring.entity.ScoreResult;
import com.smeloan.platform.scoring.repository.ScoreResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Default implementation of {@link ScoringService}, delegating scoring logic
 * to {@link ScoreCalculator}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScoringServiceImpl implements ScoringService {

    private final ScoreCalculator scoreCalculator;
    private final ScoreResultRepository scoreResultRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public ScoreResultResponse score(Long applicationId) {
        ScoreResult result = scoreCalculator.calculate(applicationId);
        return toResponse(result);
    }

    @Override
    public SimulateScoreResponse simulate(SimulateScoreRequest request) {
        return scoreCalculator.simulate(request);
    }

    @Override
    @Transactional(readOnly = true)
    public ScoreResultResponse getResult(Long applicationId) {
        ScoreResult result = scoreResultRepository.findByLoanApplicationId(applicationId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "ScoreResult not found for application id: " + applicationId));
        return toResponse(result);
    }

    // ------------------------------------------------------------------

    private ScoreResultResponse toResponse(ScoreResult result) {
        Map<String, Object> breakdown = null;
        if (result.getBreakdown() != null) {
            try {
                breakdown = objectMapper.readValue(
                    result.getBreakdown(), new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                log.warn("Failed to parse score breakdown JSON", e);
            }
        }
        return new ScoreResultResponse(
            result.getScore(),
            result.getDecision().name(),
            result.getScoredAt(),
            breakdown
        );
    }
}
