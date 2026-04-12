package com.smeloan.platform.externalcheck.entity;

import com.smeloan.platform.application.entity.LoanApplication;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

/**
 * Stores the result of a single external check (JCIC, AML, ETC, PEP) for a
 * loan application.
 */
@Entity
@Table(name = "external_check_results", indexes = {
    @Index(name = "idx_ecr_application_id", columnList = "application_id"),
    @Index(name = "idx_ecr_check_type", columnList = "check_type")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalCheckResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private LoanApplication loanApplication;

    @Enumerated(EnumType.STRING)
    @Column(name = "check_type", nullable = false, length = 16)
    private CheckType checkType;

    @Column(name = "provider", length = 128)
    private String provider;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    @Builder.Default
    private CheckStatus status = CheckStatus.PENDING;

    /** Raw JSON request payload sent to the provider. */
    @Column(name = "request_payload", columnDefinition = "jsonb")
    private String requestPayload;

    /** Raw JSON response payload received from the provider. */
    @Column(name = "response_payload", columnDefinition = "jsonb")
    private String responsePayload;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", length = 16)
    private RiskLevel riskLevel;

    @Column(name = "error_message", length = 1024)
    private String errorMessage;

    @Column(name = "checked_at")
    private OffsetDateTime checkedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
