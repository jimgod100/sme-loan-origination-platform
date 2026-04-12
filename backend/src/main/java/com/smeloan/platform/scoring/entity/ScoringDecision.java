package com.smeloan.platform.scoring.entity;

/**
 * Automated decision produced by the scoring model.
 */
public enum ScoringDecision {

    /** Score is high enough (>= 70) for automatic approval without manager review. */
    AUTO_APPROVE,

    /** Score is too low (<= 40) for automatic rejection without manager review. */
    AUTO_REJECT,

    /** Score falls in the middle band (41–69); the application requires manual review. */
    NEED_MANUAL_REVIEW
}
