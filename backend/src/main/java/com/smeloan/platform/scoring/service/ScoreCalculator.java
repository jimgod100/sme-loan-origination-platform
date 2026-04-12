package com.smeloan.platform.scoring.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smeloan.platform.application.entity.ApplicationStatus;
import com.smeloan.platform.application.entity.Customer;
import com.smeloan.platform.application.entity.LoanApplication;
import com.smeloan.platform.application.repository.LoanApplicationRepository;
import com.smeloan.platform.common.exception.BusinessException;
import com.smeloan.platform.common.exception.ResourceNotFoundException;
import com.smeloan.platform.externalcheck.entity.CheckType;
import com.smeloan.platform.externalcheck.entity.ExternalCheckResult;
import com.smeloan.platform.externalcheck.entity.RiskLevel;
import com.smeloan.platform.externalcheck.repository.ExternalCheckResultRepository;
import com.smeloan.platform.scoring.dto.SimulateScoreRequest;
import com.smeloan.platform.scoring.dto.SimulateScoreResponse;
import com.smeloan.platform.scoring.entity.ScoreResult;
import com.smeloan.platform.scoring.entity.ScoringDecision;
import com.smeloan.platform.scoring.repository.ScoreResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Calculates the composite credit score for a loan application using a
 * weighted multi-factor model.
 *
 * <h3>Scoring model (version 1.0)</h3>
 * <table border="1">
 *   <tr><th>Component</th><th>Weight</th><th>Derivation</th></tr>
 *   <tr><td>JCIC credit score</td><td>30%</td><td>Normalised from 400–900 → 0–100</td></tr>
 *   <tr><td>Debt ratio</td><td>25%</td><td>Lower is better; capped at 1.0 then inverted</td></tr>
 *   <tr><td>Interest coverage</td><td>20%</td><td>Higher is better; capped at 5×</td></tr>
 *   <tr><td>Company age</td><td>15%</td><td>Years of operation capped at 20 years</td></tr>
 *   <tr><td>AML risk</td><td>10%</td><td>LOW=100, MEDIUM=50, HIGH=0</td></tr>
 * </table>
 *
 * <h3>Decisions</h3>
 * <ul>
 *   <li>Score &ge; 70 → AUTO_APPROVE</li>
 *   <li>Score &le; 40 → AUTO_REJECT</li>
 *   <li>41–69 → NEED_MANUAL_REVIEW</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScoreCalculator {

    private static final String MODEL_VERSION = "1.0";

    // Weights (must sum to 1.0)
    private static final double W_JCIC         = 0.30;
    private static final double W_DEBT_RATIO   = 0.25;
    private static final double W_INT_COVERAGE = 0.20;
    private static final double W_COMPANY_AGE  = 0.15;
    private static final double W_AML          = 0.10;

    private final LoanApplicationRepository applicationRepository;
    private final ExternalCheckResultRepository checkResultRepository;
    private final ScoreResultRepository scoreResultRepository;
    private final ObjectMapper objectMapper;

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    /**
     * Calculates and persists the score for the given application.
     *
     * @param applicationId target application
     * @return the persisted {@link ScoreResult}
     */
    @Transactional
    public ScoreResult calculate(Long applicationId) {
        LoanApplication app = applicationRepository.findById(applicationId)
            .orElseThrow(() -> new ResourceNotFoundException("LoanApplication", applicationId));

        List<ExternalCheckResult> checks = checkResultRepository.findByLoanApplicationId(applicationId);

        // Extract JCIC data
        int jcicScore = extractJcicScore(checks);
        double debtRatio = extractDebtRatio(checks);
        double interestCoverage = extractInterestCoverage(checks);
        RiskLevel amlRisk = extractAmlRisk(checks);
        int companyAgeYears = computeCompanyAge(app.getCustomer());

        Map<String, Object> breakdown = computeBreakdown(
            jcicScore, debtRatio, interestCoverage, companyAgeYears, amlRisk);

        int totalScore = ((Number) breakdown.get("totalScore")).intValue();
        ScoringDecision decision = decideFrom(totalScore);

        // Upsert ScoreResult
        ScoreResult scoreResult = scoreResultRepository.findByLoanApplicationId(applicationId)
            .orElseGet(() -> ScoreResult.builder().loanApplication(app).build());

        scoreResult.setScore(totalScore);
        scoreResult.setDecision(decision);
        scoreResult.setScoringModelVersion(MODEL_VERSION);
        scoreResult.setScoredAt(OffsetDateTime.now());
        scoreResult.setInputSnapshot(toJson(Map.of(
            "jcicScore", jcicScore,
            "debtRatio", debtRatio,
            "interestCoverage", interestCoverage,
            "companyAgeYears", companyAgeYears,
            "amlRiskLevel", amlRisk.name()
        )));
        scoreResult.setBreakdown(toJson(breakdown));
        scoreResultRepository.save(scoreResult);

        // Update application status
        ApplicationStatus newStatus = switch (decision) {
            case AUTO_APPROVE      -> ApplicationStatus.AUTO_APPROVED;
            case AUTO_REJECT       -> ApplicationStatus.AUTO_REJECTED;
            case NEED_MANUAL_REVIEW -> ApplicationStatus.PENDING_REVIEW;
        };
        app.setStatus(newStatus);
        applicationRepository.save(app);

        log.info("Application {} scored={} decision={}", app.getApplicationNumber(), totalScore, decision);
        return scoreResult;
    }

    /**
     * Runs a simulation without persisting any data.
     *
     * @param req the simulation parameters
     * @return the simulated score and breakdown
     */
    public SimulateScoreResponse simulate(SimulateScoreRequest req) {
        RiskLevel amlRisk = RiskLevel.valueOf(req.amlRiskLevel());
        Map<String, Object> breakdown = computeBreakdown(
            req.jcicScore(), req.debtRatio(), req.interestCoverage(),
            req.companyAgeYears(), amlRisk);

        int totalScore = ((Number) breakdown.get("totalScore")).intValue();
        ScoringDecision decision = decideFrom(totalScore);

        return new SimulateScoreResponse(totalScore, decision.name(), breakdown);
    }

    // ------------------------------------------------------------------
    // Scoring logic
    // ------------------------------------------------------------------

    /**
     * Computes the weighted score for all components.
     *
     * @return a {@link LinkedHashMap} preserving insertion order for display
     */
    public Map<String, Object> computeBreakdown(
            int jcicScore, double debtRatio, double interestCoverage,
            int companyAgeYears, RiskLevel amlRisk) {

        // 1. JCIC: normalise 400–900 → 0–100
        double jcicComponent = Math.max(0, Math.min(100, (jcicScore - 400.0) / 500.0 * 100.0));

        // 2. Debt ratio: 0.0 = perfect (100), 1.0+ = worst (0), linear between
        double debtComponent = Math.max(0, Math.min(100, (1.0 - Math.min(debtRatio, 1.0)) * 100.0));

        // 3. Interest coverage: 0× = 0, 5× = 100, linear; anything above 5 capped
        double intCovComponent = Math.max(0, Math.min(100, (Math.min(interestCoverage, 5.0) / 5.0) * 100.0));

        // 4. Company age: 0 years = 0, 20+ years = 100, linear
        double ageComponent = Math.max(0, Math.min(100, (Math.min(companyAgeYears, 20) / 20.0) * 100.0));

        // 5. AML risk level
        double amlComponent = switch (amlRisk) {
            case LOW    -> 100.0;
            case MEDIUM -> 50.0;
            case HIGH   -> 0.0;
        };

        double rawTotal = jcicComponent * W_JCIC
            + debtComponent   * W_DEBT_RATIO
            + intCovComponent * W_INT_COVERAGE
            + ageComponent    * W_COMPANY_AGE
            + amlComponent    * W_AML;

        int totalScore = (int) Math.round(rawTotal);

        Map<String, Object> bd = new LinkedHashMap<>();
        bd.put("jcicComponent",        round2(jcicComponent));
        bd.put("jcicWeight",           W_JCIC);
        bd.put("jcicWeighted",         round2(jcicComponent * W_JCIC));
        bd.put("debtRatioComponent",   round2(debtComponent));
        bd.put("debtRatioWeight",      W_DEBT_RATIO);
        bd.put("debtRatioWeighted",    round2(debtComponent * W_DEBT_RATIO));
        bd.put("intCovComponent",      round2(intCovComponent));
        bd.put("intCovWeight",         W_INT_COVERAGE);
        bd.put("intCovWeighted",       round2(intCovComponent * W_INT_COVERAGE));
        bd.put("companyAgeComponent",  round2(ageComponent));
        bd.put("companyAgeWeight",     W_COMPANY_AGE);
        bd.put("companyAgeWeighted",   round2(ageComponent * W_COMPANY_AGE));
        bd.put("amlComponent",         round2(amlComponent));
        bd.put("amlWeight",            W_AML);
        bd.put("amlWeighted",          round2(amlComponent * W_AML));
        bd.put("rawTotal",             round2(rawTotal));
        bd.put("totalScore",           totalScore);
        return bd;
    }

    // ------------------------------------------------------------------
    // Extraction helpers
    // ------------------------------------------------------------------

    private int extractJcicScore(List<ExternalCheckResult> checks) {
        Optional<ExternalCheckResult> jcic = checks.stream()
            .filter(c -> c.getCheckType() == CheckType.JCIC)
            .findFirst();
        if (jcic.isEmpty() || jcic.get().getResponsePayload() == null) {
            return 600; // neutral default
        }
        try {
            Map<?, ?> map = objectMapper.readValue(jcic.get().getResponsePayload(), Map.class);
            Object score = map.get("creditScore");
            return score != null ? ((Number) score).intValue() : 600;
        } catch (Exception e) {
            return 600;
        }
    }

    private double extractDebtRatio(List<ExternalCheckResult> checks) {
        Optional<ExternalCheckResult> jcic = checks.stream()
            .filter(c -> c.getCheckType() == CheckType.JCIC)
            .findFirst();
        if (jcic.isEmpty() || jcic.get().getResponsePayload() == null) {
            return 0.5;
        }
        try {
            Map<?, ?> map = objectMapper.readValue(jcic.get().getResponsePayload(), Map.class);
            Object dr = map.get("debtRatio");
            return dr != null ? ((Number) dr).doubleValue() : 0.5;
        } catch (Exception e) {
            return 0.5;
        }
    }

    private double extractInterestCoverage(List<ExternalCheckResult> checks) {
        // Not directly available from fake providers; default to a neutral value
        return 2.5;
    }

    private RiskLevel extractAmlRisk(List<ExternalCheckResult> checks) {
        return checks.stream()
            .filter(c -> c.getCheckType() == CheckType.AML && c.getRiskLevel() != null)
            .findFirst()
            .map(ExternalCheckResult::getRiskLevel)
            .orElse(RiskLevel.LOW);
    }

    private int computeCompanyAge(Customer customer) {
        if (customer.getEstablishedDate() == null) return 5; // default 5 years
        return LocalDate.now().getYear() - customer.getEstablishedDate().getYear();
    }

    private ScoringDecision decideFrom(int score) {
        if (score >= 70) return ScoringDecision.AUTO_APPROVE;
        if (score <= 40) return ScoringDecision.AUTO_REJECT;
        return ScoringDecision.NEED_MANUAL_REVIEW;
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
