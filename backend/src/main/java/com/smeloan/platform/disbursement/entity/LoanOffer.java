package com.smeloan.platform.disbursement.entity;

import com.smeloan.platform.application.entity.LoanApplication;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * A formal loan offer generated for an approved loan application.
 */
@Entity
@Table(name = "loan_offers")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false, unique = true)
    private LoanApplication loanApplication;

    @Column(name = "approved_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal approvedAmount;

    /** Annual interest rate expressed as a decimal (e.g. 0.0450 = 4.50%). */
    @Column(name = "interest_rate", nullable = false, precision = 8, scale = 4)
    private BigDecimal interestRate;

    @Column(name = "term_months", nullable = false)
    private int termMonths;

    @Column(name = "monthly_payment", nullable = false, precision = 18, scale = 2)
    private BigDecimal monthlyPayment;

    @Column(name = "total_interest", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalInterest;

    @Enumerated(EnumType.STRING)
    @Column(name = "offer_status", nullable = false, length = 16)
    @Builder.Default
    private OfferStatus offerStatus = OfferStatus.PENDING;

    /** Path/key to the generated contract PDF in storage. */
    @Column(name = "contract_pdf_path", length = 1024)
    private String contractPdfPath;

    @Column(name = "offered_at")
    private OffsetDateTime offeredAt;

    @Column(name = "accepted_at")
    private OffsetDateTime acceptedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
