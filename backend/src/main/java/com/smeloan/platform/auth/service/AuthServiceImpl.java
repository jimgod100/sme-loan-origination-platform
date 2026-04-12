package com.smeloan.platform.auth.service;

import com.smeloan.platform.auth.dto.LoginRequest;
import com.smeloan.platform.auth.dto.LoginResponse;
import com.smeloan.platform.auth.dto.UserInfo;
import com.smeloan.platform.auth.entity.User;
import com.smeloan.platform.auth.repository.UserRepository;
import com.smeloan.platform.common.exception.BusinessException;
import com.smeloan.platform.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link AuthService} providing JWT-based authentication.
 *
 * <p>JWT signing/validation is stubbed with TODO placeholders — integrate
 * a library such as {@code jjwt} or {@code spring-security-oauth2-resource-server}
 * to complete the implementation.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /** Token lifetime: 8 hours expressed in seconds. */
    private static final long TOKEN_EXPIRY_SECONDS = 8 * 60 * 60L;

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
            .orElseThrow(() -> new BusinessException("Invalid username or password"));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new BusinessException("Account is disabled. Please contact the administrator.");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException("Invalid username or password");
        }

        // TODO: Replace stub token with a real JWT signed with RS256/HS256
        String token = generateStubToken(user);

        UserInfo userInfo = toUserInfo(user);
        log.info("User '{}' logged in successfully", user.getUsername());

        return new LoginResponse(token, "Bearer", TOKEN_EXPIRY_SECONDS, userInfo);
    }

    @Override
    @Transactional(readOnly = true)
    public UserInfo getCurrentUser(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        return toUserInfo(user);
    }

    // ------------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------------

    private UserInfo toUserInfo(User user) {
        return new UserInfo(
            user.getId(),
            user.getUsername(),
            user.getFullName(),
            user.getRole().name(),
            user.getDepartment()
        );
    }

    /**
     * TODO: Replace with real JWT generation (e.g. jjwt).
     * This stub encodes minimal user info for development purposes only.
     */
    private String generateStubToken(User user) {
        // Stub: base64-encode a simple payload — NOT secure for production
        String payload = user.getId() + ":" + user.getUsername() + ":" + user.getRole().name();
        return java.util.Base64.getEncoder().encodeToString(payload.getBytes());
    }
}
