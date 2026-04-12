package com.smeloan.platform.scoring.dto;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Response record for a credit scoring result.
 *
 * @param score     normalised composite score (0–100)
 * @param decision  automated decision (AUTO_APPROVE, AUTO_REJECT, NEED_MANUAL_REVIEW)
 * @param scoredAt  timestamp when scoring was completed
 * @param breakdown map of component name → weighted contribution to the total score
 */
public record ScoreResultResponse(
    int score,
    String decision,
    OffsetDateTime scoredAt,
    Map<String, Object> breakdown
) {}
