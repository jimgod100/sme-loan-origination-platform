package com.smeloan.platform.application.entity;

import com.smeloan.platform.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Represents an SME customer (business entity) applying for a loan.
 */
@Entity
@Table(name = "customers", indexes = {
    @Index(name = "idx_customers_ubn", columnList = "ubn")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer extends BaseEntity {

    /**
     * Unified Business Number — 8-digit government-issued company identifier.
     */
    @Column(name = "ubn", nullable = false, unique = true, length = 8)
    private String ubn;

    @Column(name = "company_name", nullable = false, length = 256)
    private String companyName;

    @Column(name = "industry", length = 128)
    private String industry;

    @Column(name = "established_date")
    private LocalDate establishedDate;

    @Column(name = "capital", precision = 18, scale = 2)
    private BigDecimal capital;

    @Column(name = "address", length = 512)
    private String address;

    @Column(name = "contact_phone", length = 32)
    private String contactPhone;

    @Column(name = "contact_email", length = 128)
    private String contactEmail;
}
