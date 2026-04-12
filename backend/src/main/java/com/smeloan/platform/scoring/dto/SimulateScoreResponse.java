package com.smeloan.platform.scoring.dto;

import java.util.Map;

/**
 * Response for a simulated scoring run.
 *
 * @param score     normalised composite score (0–100)
 * @param decision  automated decision string
 * @param breakdown per-component score breakdown
 */
public record SimulateScoreResponse(
    int score,
    String decision,
    Map<String, Object> breakdown
) {}
