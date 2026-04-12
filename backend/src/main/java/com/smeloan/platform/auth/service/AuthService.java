package com.smeloan.platform.auth.service;

import com.smeloan.platform.auth.dto.LoginRequest;
import com.smeloan.platform.auth.dto.LoginResponse;
import com.smeloan.platform.auth.dto.UserInfo;

/**
 * Service interface for authentication operations.
 */
public interface AuthService {

    /**
     * Authenticates a user with the provided credentials and returns a JWT token.
     *
     * @param request the login request containing username and password
     * @return a {@link LoginResponse} containing the JWT token and user info
     * @throws com.smeloan.platform.common.exception.BusinessException if credentials are invalid
     */
    LoginResponse login(LoginRequest request);

    /**
     * Returns the profile information of the currently authenticated user.
     *
     * @param username the username of the currently authenticated principal
     * @return the {@link UserInfo} for the authenticated user
     * @throws com.smeloan.platform.common.exception.ResourceNotFoundException if the user is not found
     */
    UserInfo getCurrentUser(String username);
}
