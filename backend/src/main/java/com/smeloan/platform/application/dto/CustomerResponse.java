package com.smeloan.platform.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Response record mapping a {@link com.smeloan.platform.application.entity.Customer} entity.
 *
 * @param id               customer database identifier
 * @param ubn              Unified Business Number
 * @param companyName      legal company name
 * @param industry         industry sector
 * @param establishedDate  date of incorporation
 * @param capital          registered capital
 * @param address          registered address
 * @param contactPhone     primary contact phone
 * @param contactEmail     primary contact email
 * @param createdAt        record creation timestamp
 */
public record CustomerResponse(
    Long id,
    String ubn,
    String companyName,
    String industry,
    LocalDate establishedDate,
    BigDecimal capital,
    String address,
    String contactPhone,
    String contactEmail,
    OffsetDateTime createdAt
) {}
