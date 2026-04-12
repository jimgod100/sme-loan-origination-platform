package com.smeloan.platform.application.entity;

import com.smeloan.platform.auth.entity.User;
import com.smeloan.platform.common.entity.BaseEntity;
import com.smeloan.platform.externalcheck.entity.ExternalCheckResult;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Core entity representing a single SME loan application.
 */
@Entity
@Table(name = "loan_applications", indexes = {
    @Index(name = "idx_loan_app_number", columnList = "application_number"),
    @Index(name = "idx_loan_app_status", columnList = "status"),
    @Index(name = "idx_loan_app_customer_token", columnList = "customer_token"),
    @Index(name = "idx_loan_app_rm_user", columnList = "rm_user_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanApplication extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rm_user_id", nullable = false)
    private User rmUser;

    @Column(name = "application_number", nullable = false, unique = true, length = 32)
    private String applicationNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    @Builder.Default
    private ApplicationStatus status = ApplicationStatus.DRAFT;

    @Column(name = "requested_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal requestedAmount;

    @Column(name = "requested_term_months", nullable = false)
    private int requestedTermMonths;

    @Column(name = "loan_purpose", length = 512)
    private String loanPurpose;

    @Column(name = "collateral_type", length = 128)
    private String collateralType;

    @Column(name = "collateral_description", length = 1024)
    private String collateralDescription;

    /** UUID token shared with the customer for the self-service onboarding link. */
    @Column(name = "customer_token", unique = true, length = 64)
    private String customerToken;

    @Column(name = "token_expires_at")
    private OffsetDateTime tokenExpiresAt;

    /** Optional rejection reason recorded by the reviewing manager. */
    @Column(name = "rejection_reason", length = 1024)
    private String rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;

    // -------------------------------------------------------------------------
    // Associations
    // -------------------------------------------------------------------------

    @OneToMany(mappedBy = "loanApplication", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ApplicationParty> parties = new ArrayList<>();

    @OneToMany(mappedBy = "loanApplication", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Document> documents = new ArrayList<>();

    @OneToMany(mappedBy = "loanApplication", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ExternalCheckResult> externalCheckResults = new ArrayList<>();
}
