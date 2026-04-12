package com.smeloan.platform.application.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

/**
 * A party (person) associated with a loan application, such as a responsible
 * person, guarantor, or contact.
 */
@Entity
@Table(name = "application_parties", indexes = {
    @Index(name = "idx_parties_application_id", columnList = "application_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationParty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private LoanApplication loanApplication;

    @Enumerated(EnumType.STRING)
    @Column(name = "party_type", nullable = false, length = 32)
    private PartyType partyType;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    /** Government-issued identification number (ID card or passport number). */
    @Column(name = "id_number", length = 32)
    private String idNumber;

    @Column(name = "phone", length = 32)
    private String phone;

    @Column(name = "email", length = 128)
    private String email;

    @Column(name = "relationship", length = 64)
    private String relationship;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
