package com.smeloan.platform.disbursement.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A single instalment in the repayment schedule for a loan offer.
 */
@Entity
@Table(
    name = "repayment_schedules",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_repayment_offer_installment",
        columnNames = {"loan_offer_id", "installment_number"}
    ),
    indexes = {
        @Index(name = "idx_repayment_offer_id", columnList = "loan_offer_id")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepaymentSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loan_offer_id", nullable = false)
    private LoanOffer loanOffer;

    @Column(name = "installment_number", nullable = false)
    private int installmentNumber;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    /** Principal portion of this instalment. */
    @Column(name = "principal", nullable = false, precision = 18, scale = 2)
    private BigDecimal principal;

    /** Interest portion of this instalment. */
    @Column(name = "interest", nullable = false, precision = 18, scale = 2)
    private BigDecimal interest;

    /** Remaining outstanding balance after this payment is applied. */
    @Column(name = "balance", nullable = false, precision = 18, scale = 2)
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    @Builder.Default
    private RepaymentStatus status = RepaymentStatus.PENDING;
}
