package com.smeloan.platform.auth.controller;

import com.smeloan.platform.auth.dto.LoginRequest;
import com.smeloan.platform.auth.dto.LoginResponse;
import com.smeloan.platform.auth.dto.UserInfo;
import com.smeloan.platform.auth.service.AuthService;
import com.smeloan.platform.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication endpoints.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Authenticates a user and returns a JWT access token.
     *
     * @param request the login credentials
     * @return a JWT token and basic user info
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Login successful", response));
    }

    /**
     * Returns the profile of the currently authenticated user.
     *
     * @param authentication the Spring Security authentication context
     * @return the current user's profile
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserInfo>> getCurrentUser(Authentication authentication) {
        UserInfo userInfo = authService.getCurrentUser(authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok(userInfo));
    }
}
