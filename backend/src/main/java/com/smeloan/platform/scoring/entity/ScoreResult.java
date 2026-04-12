package com.smeloan.platform.scoring.entity;

import com.smeloan.platform.application.entity.LoanApplication;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

/**
 * Stores the credit scoring result for a single loan application.
 * There is at most one ScoreResult per application (the most recent scoring run).
 */
@Entity
@Table(name = "score_results")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoreResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false, unique = true)
    private LoanApplication loanApplication;

    /** Normalised composite score in the range 0–100. */
    @Column(name = "score", nullable = false)
    private int score;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 32)
    private ScoringDecision decision;

    @Column(name = "scoring_model_version", length = 32)
    private String scoringModelVersion;

    /** JSON snapshot of the input features used for this scoring run. */
    @Column(name = "input_snapshot", columnDefinition = "jsonb")
    private String inputSnapshot;

    /** JSON object mapping score component names to their individual weighted values. */
    @Column(name = "breakdown", columnDefinition = "jsonb")
    private String breakdown;

    @Column(name = "scored_at")
    private OffsetDateTime scoredAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
