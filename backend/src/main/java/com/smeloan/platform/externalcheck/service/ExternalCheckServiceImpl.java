package com.smeloan.platform.externalcheck.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smeloan.platform.application.entity.ApplicationStatus;
import com.smeloan.platform.application.entity.LoanApplication;
import com.smeloan.platform.application.repository.ApplicationPartyRepository;
import com.smeloan.platform.application.repository.LoanApplicationRepository;
import com.smeloan.platform.common.exception.ResourceNotFoundException;
import com.smeloan.platform.externalcheck.dto.CheckResultResponse;
import com.smeloan.platform.externalcheck.entity.*;
import com.smeloan.platform.externalcheck.provider.*;
import com.smeloan.platform.externalcheck.repository.ExternalCheckResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Default implementation of {@link ExternalCheckService}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalCheckServiceImpl implements ExternalCheckService {

    private final LoanApplicationRepository applicationRepository;
    private final ApplicationPartyRepository partyRepository;
    private final ExternalCheckResultRepository checkResultRepository;
    private final CreditBureauClient creditBureauClient;
    private final AmlScreeningClient amlScreeningClient;
    private final BusinessRegistryClient businessRegistryClient;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    @Override
    @Async("externalCheckExecutor")
    @Transactional
    public void executeAllChecks(Long applicationId) {
        LoanApplication app = findApp(applicationId);

        // Create PENDING placeholder records
        ExternalCheckResult jcicResult  = createPending(app, CheckType.JCIC,  creditBureauClient.getProviderName());
        ExternalCheckResult amlResult   = createPending(app, CheckType.AML,   amlScreeningClient.getProviderName());
        ExternalCheckResult etcResult   = createPending(app, CheckType.ETC,   businessRegistryClient.getProviderName());

        // Execute checks (each runs in the calling async thread sequentially for simplicity;
        // use CompletableFuture.allOf for true parallelism when desired)
        runJcicCheck(app, jcicResult);
        runAmlCheck(app, amlResult);
        runEtcCheck(app, etcResult);

        evaluateFinalStatus(applicationId);
    }

    @Override
    @Async("externalCheckExecutor")
    @Transactional
    public void retryCheck(Long applicationId, CheckType checkType) {
        LoanApplication app = findApp(applicationId);
        ExternalCheckResult existing = checkResultRepository
            .findByLoanApplicationIdAndCheckType(applicationId, checkType)
            .orElseGet(() -> createPending(app, checkType, resolveProviderName(checkType)));

        existing.setStatus(CheckStatus.PENDING);
        existing.setErrorMessage(null);
        checkResultRepository.save(existing);

        switch (checkType) {
            case JCIC -> runJcicCheck(app, existing);
            case AML  -> runAmlCheck(app, existing);
            case ETC  -> runEtcCheck(app, existing);
            case PEP  -> runAmlCheck(app, existing); // PEP reuses AML provider
        }

        evaluateFinalStatus(applicationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CheckResultResponse> getCheckResults(Long applicationId) {
        return checkResultRepository.findByLoanApplicationId(applicationId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    // ------------------------------------------------------------------
    // Check runners
    // ------------------------------------------------------------------

    private void runJcicCheck(LoanApplication app, ExternalCheckResult record) {
        try {
            String idNumber = partyRepository.findByLoanApplicationId(app.getId())
                .stream()
                .findFirst()
                .map(p -> p.getIdNumber())
                .orElse("");

            CreditCheckRequest req = new CreditCheckRequest(
                app.getCustomer().getUbn(), idNumber, app.getCustomer().getCompanyName());

            record.setRequestPayload(toJson(req));
            CreditCheckResult result = creditBureauClient.query(req);

            record.setStatus(CheckStatus.SUCCESS);
            record.setResponsePayload(result.rawResponse());
            record.setRiskLevel(mapCreditScoreToRisk(result.creditScore()));
            record.setCheckedAt(OffsetDateTime.now());

            log.info("[JCIC] applicationId={} score={} risk={}",
                app.getId(), result.creditScore(), record.getRiskLevel());
        } catch (Exception e) {
            log.error("[JCIC] Check failed for applicationId={}: {}", app.getId(), e.getMessage());
            record.setStatus(CheckStatus.FAILED);
            record.setErrorMessage(e.getMessage());
            record.setCheckedAt(OffsetDateTime.now());
        }
        checkResultRepository.save(record);
    }

    private void runAmlCheck(LoanApplication app, ExternalCheckResult record) {
        try {
            String name = partyRepository.findByLoanApplicationId(app.getId())
                .stream().findFirst()
                .map(p -> p.getName())
                .orElse(app.getCustomer().getCompanyName());

            AmlScreenRequest req = new AmlScreenRequest(name, null, app.getCustomer().getUbn());
            record.setRequestPayload(toJson(req));

            AmlCheckResult result = amlScreeningClient.screen(req);

            record.setStatus(CheckStatus.SUCCESS);
            record.setResponsePayload(result.rawResponse());
            record.setRiskLevel(result.riskLevel());
            record.setCheckedAt(OffsetDateTime.now());

            log.info("[AML] applicationId={} riskLevel={} pepMatch={}",
                app.getId(), result.riskLevel(), result.pepMatch());
        } catch (Exception e) {
            log.error("[AML] Check failed for applicationId={}: {}", app.getId(), e.getMessage());
            record.setStatus(CheckStatus.FAILED);
            record.setErrorMessage(e.getMessage());
            record.setCheckedAt(OffsetDateTime.now());
        }
        checkResultRepository.save(record);
    }

    private void runEtcCheck(LoanApplication app, ExternalCheckResult record) {
        try {
            record.setRequestPayload(toJson(Map.of("ubn", app.getCustomer().getUbn())));
            BusinessRegistryResult result = businessRegistryClient.lookup(app.getCustomer().getUbn());

            record.setStatus(CheckStatus.SUCCESS);
            record.setResponsePayload(result.rawResponse());
            // ACTIVE = LOW risk, SUSPENDED = MEDIUM, DISSOLVED = HIGH
            record.setRiskLevel(switch (result.status()) {
                case "SUSPENDED" -> RiskLevel.MEDIUM;
                case "DISSOLVED" -> RiskLevel.HIGH;
                default -> RiskLevel.LOW;
            });
            record.setCheckedAt(OffsetDateTime.now());

            log.info("[ETC] applicationId={} companyStatus={}", app.getId(), result.status());
        } catch (Exception e) {
            log.error("[ETC] Check failed for applicationId={}: {}", app.getId(), e.getMessage());
            record.setStatus(CheckStatus.FAILED);
            record.setErrorMessage(e.getMessage());
            record.setCheckedAt(OffsetDateTime.now());
        }
        checkResultRepository.save(record);
    }

    // ------------------------------------------------------------------
    // Status evaluation
    // ------------------------------------------------------------------

    private void evaluateFinalStatus(Long applicationId) {
        long total    = checkResultRepository.countByLoanApplicationId(applicationId);
        long failed   = checkResultRepository.countByLoanApplicationIdAndStatus(applicationId, CheckStatus.FAILED);
        long timeout  = checkResultRepository.countByLoanApplicationIdAndStatus(applicationId, CheckStatus.TIMEOUT);
        long success  = checkResultRepository.countByLoanApplicationIdAndStatus(applicationId, CheckStatus.SUCCESS);

        LoanApplication app = findApp(applicationId);

        if ((failed + timeout) > 0) {
            app.setStatus(ApplicationStatus.CHECK_FAILED);
            log.warn("Application {} has {} failed checks", app.getApplicationNumber(), failed + timeout);
        } else if (success == total) {
            app.setStatus(ApplicationStatus.CHECKS_COMPLETED);
            log.info("Application {} all checks completed successfully", app.getApplicationNumber());
        }

        applicationRepository.save(app);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private ExternalCheckResult createPending(LoanApplication app, CheckType type, String provider) {
        ExternalCheckResult r = ExternalCheckResult.builder()
            .loanApplication(app)
            .checkType(type)
            .provider(provider)
            .status(CheckStatus.PENDING)
            .build();
        return checkResultRepository.save(r);
    }

    private RiskLevel mapCreditScoreToRisk(int score) {
        if (score >= 700) return RiskLevel.LOW;
        if (score >= 500) return RiskLevel.MEDIUM;
        return RiskLevel.HIGH;
    }

    private String resolveProviderName(CheckType type) {
        return switch (type) {
            case JCIC -> creditBureauClient.getProviderName();
            case AML, PEP -> amlScreeningClient.getProviderName();
            case ETC -> businessRegistryClient.getProviderName();
        };
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private LoanApplication findApp(Long id) {
        return applicationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("LoanApplication", id));
    }

    private CheckResultResponse toResponse(ExternalCheckResult r) {
        return new CheckResultResponse(
            r.getId(),
            r.getCheckType().name(),
            r.getProvider(),
            r.getStatus().name(),
            r.getRiskLevel() != null ? r.getRiskLevel().name() : null,
            r.getErrorMessage(),
            r.getCheckedAt(),
            r.getCreatedAt()
        );
    }
}
