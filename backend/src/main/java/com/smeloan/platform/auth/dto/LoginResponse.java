package com.smeloan.platform.auth.dto;

/**
 * Response returned upon successful authentication.
 *
 * @param accessToken the JWT access token
 * @param tokenType   the token type, typically "Bearer"
 * @param expiresIn   token lifetime in seconds
 * @param user        summary of the authenticated user
 */
public record LoginResponse(
    String accessToken,
    String tokenType,
    Long expiresIn,
    UserInfo user
) {}
