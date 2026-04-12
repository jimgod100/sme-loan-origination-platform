package com.smeloan.platform.onboarding.service;

import com.smeloan.platform.application.dto.ApplicationDetailResponse;
import com.smeloan.platform.application.entity.*;
import com.smeloan.platform.application.event.ReadyForChecksEvent;
import com.smeloan.platform.application.repository.*;
import com.smeloan.platform.application.service.LoanApplicationService;
import com.smeloan.platform.common.exception.BusinessException;
import com.smeloan.platform.common.exception.ResourceNotFoundException;
import com.smeloan.platform.onboarding.dto.OnboardingStatusResponse;
import com.smeloan.platform.onboarding.dto.SubmitConsentRequest;
import com.smeloan.platform.onboarding.dto.SubmitPartiesRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Default implementation of {@link OnboardingService}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OnboardingServiceImpl implements OnboardingService {

    private final LoanApplicationRepository applicationRepository;
    private final ApplicationPartyRepository partyRepository;
    private final DocumentRepository documentRepository;
    private final LoanApplicationService loanApplicationService;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.storage.upload-dir:./uploads}")
    private String uploadDir;

    // ------------------------------------------------------------------
    // Token status
    // ------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public OnboardingStatusResponse getTokenStatus(String token) {
        return applicationRepository.findByCustomerToken(token)
            .map(app -> {
                boolean expired = app.getTokenExpiresAt() != null
                    && OffsetDateTime.now().isAfter(app.getTokenExpiresAt());
                boolean valid = !expired;
                return new OnboardingStatusResponse(
                    valid,
                    app.getApplicationNumber(),
                    app.getCustomer().getCompanyName(),
                    app.getStatus().name(),
                    app.getTokenExpiresAt()
                );
            })
            .orElse(new OnboardingStatusResponse(false, null, null, null, null));
    }

    // ------------------------------------------------------------------
    // Get application
    // ------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public ApplicationDetailResponse getApplicationByToken(String token) {
        LoanApplication app = validateAndGetApp(token);
        return loanApplicationService.getApplication(app.getId());
    }

    // ------------------------------------------------------------------
    // Submit parties
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public void submitParties(String token, SubmitPartiesRequest request) {
        LoanApplication app = validateAndGetApp(token);

        // Replace all existing parties
        partyRepository.deleteByLoanApplicationId(app.getId());

        for (SubmitPartiesRequest.PartyRequest pr : request.parties()) {
            PartyType partyType;
            try {
                partyType = PartyType.valueOf(pr.partyType().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BusinessException("Invalid party type: " + pr.partyType());
            }

            ApplicationParty party = ApplicationParty.builder()
                .loanApplication(app)
                .partyType(partyType)
                .name(pr.name())
                .idNumber(pr.idNumber())
                .phone(pr.phone())
                .email(pr.email())
                .relationship(pr.relationship())
                .build();
            partyRepository.save(party);
        }

        log.info("Saved {} parties for application {}", request.parties().size(), app.getApplicationNumber());
    }

    // ------------------------------------------------------------------
    // Upload document
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public void uploadDocument(String token, DocumentType type, MultipartFile file) {
        LoanApplication app = validateAndGetApp(token);

        if (file == null || file.isEmpty()) {
            throw new BusinessException("Uploaded file is empty");
        }

        try {
            // Ensure upload directory exists
            Path uploadPath = Paths.get(uploadDir, "application-" + app.getId());
            Files.createDirectories(uploadPath);

            // Generate a unique file name to avoid collisions
            String originalName = file.getOriginalFilename() != null
                ? file.getOriginalFilename() : "unknown";
            String storedName = UUID.randomUUID() + "_" + originalName;
            Path destination = uploadPath.resolve(storedName);

            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

            Document document = Document.builder()
                .loanApplication(app)
                .documentType(type)
                .fileName(originalName)
                .filePath(destination.toString())
                .fileSize(file.getSize())
                .mimeType(file.getContentType())
                .uploadedBy("customer")
                .build();
            documentRepository.save(document);

            log.info("Stored document {} ({}) for application {}",
                originalName, type, app.getApplicationNumber());
        } catch (IOException e) {
            throw new BusinessException("Failed to save uploaded file: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Submit consent
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public void submitConsent(String token, SubmitConsentRequest request) {
        LoanApplication app = validateAndGetApp(token);

        if (!request.consentJcic() || !request.consentDataCollection() || !request.consentAml()) {
            throw new BusinessException(
                "All three consents (JCIC, data collection, AML) must be granted to proceed.");
        }

        app.setStatus(ApplicationStatus.READY_FOR_CHECKS);
        applicationRepository.save(app);

        eventPublisher.publishEvent(new ReadyForChecksEvent(app.getId()));
        log.info("Consent submitted for application {}; status → READY_FOR_CHECKS", app.getApplicationNumber());
    }

    // ------------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------------

    /**
     * Retrieves the application for the token and validates it is not expired.
     */
    private LoanApplication validateAndGetApp(String token) {
        LoanApplication app = applicationRepository.findByCustomerToken(token)
            .orElseThrow(() -> new BusinessException("Invalid onboarding token"));

        if (app.getTokenExpiresAt() != null && OffsetDateTime.now().isAfter(app.getTokenExpiresAt())) {
            app.setStatus(ApplicationStatus.EXPIRED);
            applicationRepository.save(app);
            throw new BusinessException("Onboarding token has expired");
        }

        return app;
    }
}
