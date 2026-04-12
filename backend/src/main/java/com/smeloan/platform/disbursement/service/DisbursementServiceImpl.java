package com.smeloan.platform.disbursement.service;

import com.smeloan.platform.application.entity.ApplicationStatus;
import com.smeloan.platform.application.entity.LoanApplication;
import com.smeloan.platform.application.repository.LoanApplicationRepository;
import com.smeloan.platform.common.exception.BusinessException;
import com.smeloan.platform.common.exception.ResourceNotFoundException;
import com.smeloan.platform.disbursement.dto.GenerateOfferRequest;
import com.smeloan.platform.disbursement.dto.LoanOfferResponse;
import com.smeloan.platform.disbursement.dto.RepaymentScheduleResponse;
import com.smeloan.platform.disbursement.entity.LoanOffer;
import com.smeloan.platform.disbursement.entity.OfferStatus;
import com.smeloan.platform.disbursement.entity.RepaymentSchedule;
import com.smeloan.platform.disbursement.entity.RepaymentStatus;
import com.smeloan.platform.disbursement.repository.LoanOfferRepository;
import com.smeloan.platform.disbursement.repository.RepaymentScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Default implementation of {@link DisbursementService}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DisbursementServiceImpl implements DisbursementService {

    private static final MathContext MC = new MathContext(12, RoundingMode.HALF_UP);
    private static final int SCALE = 2;

    private final LoanApplicationRepository applicationRepository;
    private final LoanOfferRepository loanOfferRepository;
    private final RepaymentScheduleRepository repaymentScheduleRepository;

    // ------------------------------------------------------------------
    // Generate offer
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public LoanOfferResponse generateOffer(Long applicationId, GenerateOfferRequest request) {
        LoanApplication app = findApp(applicationId);

        if (app.getStatus() != ApplicationStatus.APPROVED
                && app.getStatus() != ApplicationStatus.AUTO_APPROVED) {
            throw new BusinessException(
                "Application must be APPROVED to generate an offer, current: " + app.getStatus());
        }

        // Delete existing offer if regenerating
        loanOfferRepository.findByLoanApplicationId(applicationId).ifPresent(existing -> {
            repaymentScheduleRepository.deleteByLoanOfferId(existing.getId());
            loanOfferRepository.delete(existing);
        });

        BigDecimal principal  = request.approvedAmount();
        BigDecimal annualRate = request.interestRate();
        int n = request.termMonths();

        BigDecimal monthlyRate = annualRate.divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);
        BigDecimal monthlyPayment = calculateMonthlyPayment(principal, monthlyRate, n);
        BigDecimal totalPayment   = monthlyPayment.multiply(BigDecimal.valueOf(n)).setScale(SCALE, RoundingMode.HALF_UP);
        BigDecimal totalInterest  = totalPayment.subtract(principal).setScale(SCALE, RoundingMode.HALF_UP);

        LoanOffer offer = LoanOffer.builder()
            .loanApplication(app)
            .approvedAmount(principal)
            .interestRate(annualRate)
            .termMonths(n)
            .monthlyPayment(monthlyPayment)
            .totalInterest(totalInterest)
            .offerStatus(OfferStatus.PENDING)
            .offeredAt(OffsetDateTime.now())
            .build();
        loanOfferRepository.save(offer);

        // Generate repayment schedule
        List<RepaymentSchedule> schedule = buildSchedule(offer, principal, monthlyRate, monthlyPayment, n);
        repaymentScheduleRepository.saveAll(schedule);

        // Update application status
        app.setStatus(ApplicationStatus.OFFER_GENERATED);
        applicationRepository.save(app);

        log.info("Generated offer for application {} monthlyPayment={} totalInterest={}",
            app.getApplicationNumber(), monthlyPayment, totalInterest);

        return toOfferResponse(offer);
    }

    // ------------------------------------------------------------------
    // Get offer
    // ------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public LoanOfferResponse getOffer(Long applicationId) {
        LoanOffer offer = loanOfferRepository.findByLoanApplicationId(applicationId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "LoanOffer not found for application id: " + applicationId));
        return toOfferResponse(offer);
    }

    // ------------------------------------------------------------------
    // Accept offer
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public void acceptOffer(Long applicationId) {
        LoanOffer offer = loanOfferRepository.findByLoanApplicationId(applicationId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "LoanOffer not found for application id: " + applicationId));

        if (offer.getOfferStatus() != OfferStatus.PENDING) {
            throw new BusinessException("Offer is not in PENDING status: " + offer.getOfferStatus());
        }

        offer.setOfferStatus(OfferStatus.ACCEPTED);
        offer.setAcceptedAt(OffsetDateTime.now());
        loanOfferRepository.save(offer);

        log.info("Offer for application {} accepted", applicationId);
    }

    // ------------------------------------------------------------------
    // Repayment schedule
    // ------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<RepaymentScheduleResponse> getRepaymentSchedule(Long applicationId) {
        LoanOffer offer = loanOfferRepository.findByLoanApplicationId(applicationId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "LoanOffer not found for application id: " + applicationId));

        return repaymentScheduleRepository.findByLoanOfferIdOrderByInstallmentNumber(offer.getId())
            .stream()
            .map(s -> new RepaymentScheduleResponse(
                s.getInstallmentNumber(),
                s.getDueDate(),
                s.getPrincipal(),
                s.getInterest(),
                s.getPrincipal().add(s.getInterest()),
                s.getBalance(),
                s.getStatus().name()
            ))
            .toList();
    }

    // ------------------------------------------------------------------
    // Contract PDF (stub)
    // ------------------------------------------------------------------

    @Override
    public byte[] generateContractPdf(Long applicationId) {
        // TODO: Integrate a PDF generation library (e.g. iText, JasperReports, OpenPDF)
        // This stub returns a minimal placeholder PDF byte array.
        String placeholder = "LOAN CONTRACT - Application ID: " + applicationId + "\n[PDF generation not yet implemented]";
        return placeholder.getBytes();
    }

    // ------------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------------

    /**
     * Standard amortisation formula: M = P * [r(1+r)^n] / [(1+r)^n - 1]
     *
     * @param principal   loan principal amount P
     * @param monthlyRate monthly interest rate r
     * @param n           number of monthly instalments
     * @return the fixed monthly payment M
     */
    private BigDecimal calculateMonthlyPayment(BigDecimal principal, BigDecimal monthlyRate, int n) {
        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            // Zero interest: simple division
            return principal.divide(BigDecimal.valueOf(n), SCALE, RoundingMode.HALF_UP);
        }
        BigDecimal onePlusR   = BigDecimal.ONE.add(monthlyRate);
        BigDecimal onePlusRn  = onePlusR.pow(n, MC);
        BigDecimal numerator  = monthlyRate.multiply(onePlusRn, MC);
        BigDecimal denominator = onePlusRn.subtract(BigDecimal.ONE);
        return principal.multiply(numerator.divide(denominator, 10, RoundingMode.HALF_UP))
            .setScale(SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Builds the full amortisation table for the given offer.
     */
    private List<RepaymentSchedule> buildSchedule(
            LoanOffer offer,
            BigDecimal principal,
            BigDecimal monthlyRate,
            BigDecimal monthlyPayment,
            int n) {

        List<RepaymentSchedule> schedule = new ArrayList<>(n);
        BigDecimal balance = principal;
        LocalDate dueDate = LocalDate.now().plusMonths(1);

        for (int i = 1; i <= n; i++) {
            BigDecimal interestPortion = balance.multiply(monthlyRate).setScale(SCALE, RoundingMode.HALF_UP);
            BigDecimal principalPortion;

            if (i == n) {
                // Last instalment: clear remaining balance
                principalPortion = balance;
            } else {
                principalPortion = monthlyPayment.subtract(interestPortion).setScale(SCALE, RoundingMode.HALF_UP);
                if (principalPortion.compareTo(BigDecimal.ZERO) < 0) {
                    principalPortion = BigDecimal.ZERO;
                }
            }

            balance = balance.subtract(principalPortion).setScale(SCALE, RoundingMode.HALF_UP);
            if (balance.compareTo(BigDecimal.ZERO) < 0) {
                balance = BigDecimal.ZERO;
            }

            schedule.add(RepaymentSchedule.builder()
                .loanOffer(offer)
                .installmentNumber(i)
                .dueDate(dueDate)
                .principal(principalPortion)
                .interest(interestPortion)
                .balance(balance)
                .status(RepaymentStatus.PENDING)
                .build());

            dueDate = dueDate.plusMonths(1);
        }
        return schedule;
    }

    private LoanApplication findApp(Long id) {
        return applicationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("LoanApplication", id));
    }

    private LoanOfferResponse toOfferResponse(LoanOffer offer) {
        return new LoanOfferResponse(
            offer.getId(),
            offer.getLoanApplication().getId(),
            offer.getApprovedAmount(),
            offer.getInterestRate(),
            offer.getTermMonths(),
            offer.getMonthlyPayment(),
            offer.getTotalInterest(),
            offer.getOfferStatus().name(),
            offer.getOfferedAt(),
            offer.getAcceptedAt(),
            offer.getCreatedAt()
        );
    }
}
