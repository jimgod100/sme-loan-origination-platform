package com.smeloan.platform.auth.dto;

/**
 * Compact user profile information returned in auth responses.
 *
 * @param id         the user's unique identifier
 * @param username   the user's login name
 * @param fullName   the user's display name
 * @param role       the user's role (RM, MANAGER, ADMIN)
 * @param department the organisational department the user belongs to
 */
public record UserInfo(
    Long id,
    String username,
    String fullName,
    String role,
    String department
) {}
