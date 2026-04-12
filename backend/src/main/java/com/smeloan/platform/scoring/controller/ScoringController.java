package com.smeloan.platform.scoring.controller;

import com.smeloan.platform.common.dto.ApiResponse;
import com.smeloan.platform.scoring.dto.ScoreResultResponse;
import com.smeloan.platform.scoring.dto.SimulateScoreRequest;
import com.smeloan.platform.scoring.dto.SimulateScoreResponse;
import com.smeloan.platform.scoring.service.ScoringService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for credit scoring operations.
 */
@RestController
@RequiredArgsConstructor
public class ScoringController {

    private final ScoringService scoringService;

    /**
     * Returns the stored scoring result for the given application.
     */
    @GetMapping("/api/applications/{id}/score-result")
    public ResponseEntity<ApiResponse<ScoreResultResponse>> getScoreResult(@PathVariable Long id) {
        ScoreResultResponse result = scoringService.getResult(id);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * Simulates a credit score for hypothetical inputs without persisting data.
     * Useful for what-if analysis.
     */
    @PostMapping("/api/scoring/simulate")
    public ResponseEntity<ApiResponse<SimulateScoreResponse>> simulate(
            @Valid @RequestBody SimulateScoreRequest request) {
        SimulateScoreResponse response = scoringService.simulate(request);
        return ResponseEntity.ok(ApiResponse.ok("Simulation complete", response));
    }
}
