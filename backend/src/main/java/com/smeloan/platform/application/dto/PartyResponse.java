package com.smeloan.platform.application.dto;

import java.time.OffsetDateTime;

/**
 * Response record for an application party.
 *
 * @param id           party database identifier
 * @param partyType    type of party (RESPONSIBLE_PERSON, GUARANTOR, CONTACT)
 * @param name         party's full name
 * @param idNumber     government-issued ID number
 * @param phone        contact phone number
 * @param email        contact email address
 * @param relationship relationship to the company or other parties
 * @param createdAt    record creation timestamp
 */
public record PartyResponse(
    Long id,
    String partyType,
    String name,
    String idNumber,
    String phone,
    String email,
    String relationship,
    OffsetDateTime createdAt
) {}
