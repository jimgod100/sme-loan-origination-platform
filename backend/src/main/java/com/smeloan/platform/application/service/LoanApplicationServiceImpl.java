package com.smeloan.platform.application.service;

import com.smeloan.platform.application.dto.*;
import com.smeloan.platform.application.entity.*;
import com.smeloan.platform.application.event.ReadyForChecksEvent;
import com.smeloan.platform.application.event.TriggerScoringEvent;
import com.smeloan.platform.application.repository.*;
import com.smeloan.platform.auth.entity.User;
import com.smeloan.platform.auth.repository.UserRepository;
import com.smeloan.platform.common.exception.BusinessException;
import com.smeloan.platform.common.exception.ResourceNotFoundException;
import com.smeloan.platform.externalcheck.dto.CheckResultResponse;
import com.smeloan.platform.externalcheck.repository.ExternalCheckResultRepository;
import com.smeloan.platform.scoring.dto.ScoreResultResponse;
import com.smeloan.platform.scoring.repository.ScoreResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Default implementation of {@link LoanApplicationService}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoanApplicationServiceImpl implements LoanApplicationService {

    private final LoanApplicationRepository applicationRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final ApplicationPartyRepository partyRepository;
    private final DocumentRepository documentRepository;
    private final ExternalCheckResultRepository checkResultRepository;
    private final ScoreResultRepository scoreResultRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.onboarding.base-url:http://localhost:3000/onboarding}")
    private String onboardingBaseUrl;

    /** Sequence suffix for application number generation within the current minute. */
    private static final AtomicLong SEQUENCE = new AtomicLong(0);

    // ------------------------------------------------------------------
    // Create
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public ApplicationResponse createApplication(CreateApplicationRequest request, Long rmUserId) {
        User rm = userRepository.findById(rmUserId)
            .orElseThrow(() -> new ResourceNotFoundException("User", rmUserId));

        Customer customer = customerRepository.findByUbn(request.ubn())
            .orElseGet(() -> {
                Customer c = Customer.builder()
                    .ubn(request.ubn())
                    .companyName(request.companyName())
                    .industry(request.industry())
                    .establishedDate(request.establishedDate())
                    .capital(request.capital())
                    .address(request.address())
                    .contactPhone(request.contactPhone())
                    .contactEmail(request.contactEmail())
                    .build();
                return customerRepository.save(c);
            });

        // If customer already existed, update mutable fields in case they changed
        if (customer.getId() != null) {
            customer.setCompanyName(request.companyName());
            if (request.industry() != null) customer.setIndustry(request.industry());
            if (request.capital() != null) customer.setCapital(request.capital());
            if (request.contactPhone() != null) customer.setContactPhone(request.contactPhone());
            if (request.contactEmail() != null) customer.setContactEmail(request.contactEmail());
        }

        String applicationNumber = generateApplicationNumber();

        LoanApplication app = LoanApplication.builder()
            .customer(customer)
            .rmUser(rm)
            .applicationNumber(applicationNumber)
            .status(ApplicationStatus.DRAFT)
            .requestedAmount(request.requestedAmount())
            .requestedTermMonths(request.requestedTermMonths())
            .loanPurpose(request.loanPurpose())
            .collateralType(request.collateralType())
            .collateralDescription(request.collateralDescription())
            .build();

        applicationRepository.save(app);
        log.info("Created loan application {} for customer UBN {}", applicationNumber, request.ubn());

        return toApplicationResponse(app);
    }

    // ------------------------------------------------------------------
    // Read
    // ------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public ApplicationDetailResponse getApplication(Long id) {
        LoanApplication app = applicationRepository.findByIdWithDetails(id)
            .orElseThrow(() -> new ResourceNotFoundException("LoanApplication", id));
        return toDetailResponse(app);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ApplicationListResponse> listApplications(ApplicationStatus status, Pageable pageable) {
        Page<LoanApplication> page = (status != null)
            ? applicationRepository.findByStatus(status, pageable)
            : applicationRepository.findAll(pageable);
        return page.map(this::toListResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ApplicationListResponse> listApplicationsByRm(Long rmUserId, Pageable pageable) {
        return applicationRepository.findByRmUser_Id(rmUserId, pageable)
            .map(this::toListResponse);
    }

    // ------------------------------------------------------------------
    // Token generation
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public GenerateTokenResponse generateCustomerToken(Long applicationId) {
        LoanApplication app = findApp(applicationId);

        if (app.getStatus() != ApplicationStatus.DRAFT
                && app.getStatus() != ApplicationStatus.PENDING_CUSTOMER
                && app.getStatus() != ApplicationStatus.EXPIRED) {
            throw new BusinessException(
                "Cannot generate token for application in status: " + app.getStatus());
        }

        String token = UUID.randomUUID().toString().replace("-", "");
        OffsetDateTime expiresAt = OffsetDateTime.now().plusDays(7);

        app.setCustomerToken(token);
        app.setTokenExpiresAt(expiresAt);
        app.setStatus(ApplicationStatus.PENDING_CUSTOMER);
        applicationRepository.save(app);

        String url = onboardingBaseUrl + "/" + token;
        log.info("Generated customer token for application {} expiring {}", app.getApplicationNumber(), expiresAt);

        return new GenerateTokenResponse(token, url, expiresAt);
    }

    // ------------------------------------------------------------------
    // Pipeline triggers
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public void triggerExternalChecks(Long applicationId) {
        LoanApplication app = findApp(applicationId);

        if (app.getStatus() != ApplicationStatus.READY_FOR_CHECKS
                && app.getStatus() != ApplicationStatus.CHECK_FAILED) {
            throw new BusinessException(
                "Application must be in READY_FOR_CHECKS status, current: " + app.getStatus());
        }

        app.setStatus(ApplicationStatus.CHECKING);
        applicationRepository.save(app);

        eventPublisher.publishEvent(new ReadyForChecksEvent(applicationId));
        log.info("Triggered external checks for application {}", app.getApplicationNumber());
    }

    @Override
    @Transactional
    public void triggerScoring(Long applicationId) {
        LoanApplication app = findApp(applicationId);

        if (app.getStatus() != ApplicationStatus.CHECKS_COMPLETED) {
            throw new BusinessException(
                "Application must be in CHECKS_COMPLETED status, current: " + app.getStatus());
        }

        app.setStatus(ApplicationStatus.SCORING);
        applicationRepository.save(app);

        eventPublisher.publishEvent(new TriggerScoringEvent(applicationId));
        log.info("Triggered scoring for application {}", app.getApplicationNumber());
    }

    // ------------------------------------------------------------------
    // Manager actions
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public void approveApplication(Long applicationId, Long managerId) {
        LoanApplication app = findApp(applicationId);
        User manager = userRepository.findById(managerId)
            .orElseThrow(() -> new ResourceNotFoundException("User", managerId));

        if (app.getStatus() != ApplicationStatus.PENDING_REVIEW
                && app.getStatus() != ApplicationStatus.SCORED
                && app.getStatus() != ApplicationStatus.AUTO_APPROVED) {
            throw new BusinessException(
                "Application cannot be approved in current status: " + app.getStatus());
        }

        app.setStatus(ApplicationStatus.APPROVED);
        app.setReviewedBy(manager);
        app.setReviewedAt(OffsetDateTime.now());
        applicationRepository.save(app);

        log.info("Application {} approved by manager {}", app.getApplicationNumber(), manager.getUsername());
    }

    @Override
    @Transactional
    public void rejectApplication(Long applicationId, Long managerId, String reason) {
        LoanApplication app = findApp(applicationId);
        User manager = userRepository.findById(managerId)
            .orElseThrow(() -> new ResourceNotFoundException("User", managerId));

        if (app.getStatus() != ApplicationStatus.PENDING_REVIEW
                && app.getStatus() != ApplicationStatus.SCORED
                && app.getStatus() != ApplicationStatus.AUTO_REJECTED) {
            throw new BusinessException(
                "Application cannot be rejected in current status: " + app.getStatus());
        }

        app.setStatus(ApplicationStatus.REJECTED);
        app.setRejectionReason(reason);
        app.setReviewedBy(manager);
        app.setReviewedAt(OffsetDateTime.now());
        applicationRepository.save(app);

        log.info("Application {} rejected by manager {}: {}",
            app.getApplicationNumber(), manager.getUsername(), reason);
    }

    @Override
    @Transactional
    public void disburseApplication(Long applicationId) {
        LoanApplication app = findApp(applicationId);

        if (app.getStatus() != ApplicationStatus.APPROVED
                && app.getStatus() != ApplicationStatus.OFFER_GENERATED) {
            throw new BusinessException(
                "Application must be APPROVED or OFFER_GENERATED to disburse, current: " + app.getStatus());
        }

        app.setStatus(ApplicationStatus.DISBURSED);
        applicationRepository.save(app);

        log.info("Application {} marked as DISBURSED", app.getApplicationNumber());
    }

    // ------------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------------

    private LoanApplication findApp(Long id) {
        return applicationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("LoanApplication", id));
    }

    /**
     * Generates a unique application number in the format {@code YYYYMM-XXXXX}.
     * The suffix is a zero-padded 5-digit atomic counter reset per JVM run.
     */
    private String generateApplicationNumber() {
        String yyyymm = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        long seq = SEQUENCE.incrementAndGet();
        return String.format("%s-%05d", yyyymm, seq);
    }

    private ApplicationResponse toApplicationResponse(LoanApplication app) {
        return new ApplicationResponse(
            app.getId(),
            app.getApplicationNumber(),
            app.getCustomer().getCompanyName(),
            app.getRmUser().getFullName(),
            app.getStatus().name(),
            app.getRequestedAmount(),
            app.getRequestedTermMonths(),
            app.getLoanPurpose(),
            app.getCollateralType(),
            app.getParties().size(),
            app.getDocuments().size(),
            app.getCustomerToken(),
            app.getTokenExpiresAt(),
            app.getCreatedAt(),
            app.getUpdatedAt()
        );
    }

    private ApplicationListResponse toListResponse(LoanApplication app) {
        return new ApplicationListResponse(
            app.getId(),
            app.getApplicationNumber(),
            app.getCustomer().getCompanyName(),
            app.getCustomer().getUbn(),
            app.getRmUser().getFullName(),
            app.getStatus().name(),
            app.getRequestedAmount(),
            app.getRequestedTermMonths(),
            app.getCreatedAt()
        );
    }

    private ApplicationDetailResponse toDetailResponse(LoanApplication app) {
        CustomerResponse customerResp = toCustomerResponse(app.getCustomer());

        List<PartyResponse> parties = partyRepository.findByLoanApplicationId(app.getId())
            .stream().map(this::toPartyResponse).toList();

        List<DocumentResponse> documents = documentRepository.findByLoanApplicationId(app.getId())
            .stream().map(this::toDocumentResponse).toList();

        List<CheckResultResponse> checkResults = checkResultRepository.findByLoanApplicationId(app.getId())
            .stream().map(this::toCheckResultResponse).toList();

        ScoreResultResponse scoreResult = scoreResultRepository.findByLoanApplicationId(app.getId())
            .map(sr -> new ScoreResultResponse(
                sr.getScore(),
                sr.getDecision().name(),
                sr.getScoredAt(),
                null // breakdown parsed separately if needed
            )).orElse(null);

        String reviewedByName = (app.getReviewedBy() != null) ? app.getReviewedBy().getFullName() : null;

        return new ApplicationDetailResponse(
            app.getId(),
            app.getApplicationNumber(),
            app.getStatus().name(),
            app.getRequestedAmount(),
            app.getRequestedTermMonths(),
            app.getLoanPurpose(),
            app.getCollateralType(),
            app.getCollateralDescription(),
            app.getCustomerToken(),
            app.getTokenExpiresAt(),
            app.getRejectionReason(),
            app.getReviewedAt(),
            app.getCreatedAt(),
            app.getUpdatedAt(),
            customerResp,
            app.getRmUser().getFullName(),
            reviewedByName,
            parties,
            documents,
            checkResults,
            scoreResult
        );
    }

    private CustomerResponse toCustomerResponse(Customer c) {
        return new CustomerResponse(
            c.getId(), c.getUbn(), c.getCompanyName(), c.getIndustry(),
            c.getEstablishedDate(), c.getCapital(), c.getAddress(),
            c.getContactPhone(), c.getContactEmail(), c.getCreatedAt()
        );
    }

    private PartyResponse toPartyResponse(com.smeloan.platform.application.entity.ApplicationParty p) {
        return new PartyResponse(
            p.getId(), p.getPartyType().name(), p.getName(),
            p.getIdNumber(), p.getPhone(), p.getEmail(),
            p.getRelationship(), p.getCreatedAt()
        );
    }

    private DocumentResponse toDocumentResponse(com.smeloan.platform.application.entity.Document d) {
        return new DocumentResponse(
            d.getId(), d.getDocumentType().name(), d.getFileName(),
            d.getFileSize(), d.getMimeType(), d.getUploadedBy(), d.getUploadedAt()
        );
    }

    private CheckResultResponse toCheckResultResponse(com.smeloan.platform.externalcheck.entity.ExternalCheckResult r) {
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
