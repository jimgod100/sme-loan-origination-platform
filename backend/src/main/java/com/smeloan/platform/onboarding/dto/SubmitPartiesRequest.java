package com.smeloan.platform.onboarding.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * Request payload for the customer to submit party information during onboarding.
 *
 * @param parties list of party records to be associated with the application
 */
public record SubmitPartiesRequest(

    @NotEmpty(message = "At least one party is required")
    @Valid
    List<PartyRequest> parties
) {

    /**
     * A single party record submitted during customer onboarding.
     *
     * @param partyType    type of party (RESPONSIBLE_PERSON, GUARANTOR, CONTACT)
     * @param name         party's full name
     * @param idNumber     government-issued ID number
     * @param phone        contact phone number
     * @param email        contact email address
     * @param relationship relationship to the company or loan
     */
    public record PartyRequest(
        @NotBlank(message = "Party type is required")
        String partyType,

        @NotBlank(message = "Name is required")
        String name,

        String idNumber,
        String phone,
        String email,
        String relationship
    ) {}
}
