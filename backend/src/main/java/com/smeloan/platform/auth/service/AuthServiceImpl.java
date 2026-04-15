package com.smeloan.platform.auth.service;

import com.smeloan.platform.auth.dto.LoginRequest;
import com.smeloan.platform.auth.dto.LoginResponse;
import com.smeloan.platform.auth.dto.UserInfo;
import com.smeloan.platform.auth.entity.User;
import com.smeloan.platform.auth.repository.UserRepository;
import com.smeloan.platform.common.exception.BusinessException;
import com.smeloan.platform.common.exception.ResourceNotFoundException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * Implementation of {@link AuthService} providing JWT-based authentication.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /** Token lifetime: 8 hours expressed in seconds. */
    private static final long TOKEN_EXPIRY_SECONDS = 8 * 60 * 60L;
    private static final String JWT_ISSUER = "sme-loan-platform";

    /**
     * Demo-only HMAC secret key for signing JWTs.
     * In a real deployment, this must be externalised to configuration and rotated regularly.
     */
    private static final String JWT_SECRET =
        "change-me-demo-secret-key-for-sme-loan-platform-256-bit-equivalent";

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

        String token = generateJwtToken(user);

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

    private String generateJwtToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(TOKEN_EXPIRY_SECONDS);

        SecretKey key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
            .setSubject(user.getUsername())
            .setId(user.getId().toString())
            .setIssuer(JWT_ISSUER)
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(expiry))
            .claim("role", user.getRole().name())
            .claim("fullName", user.getFullName())
            .claim("department", user.getDepartment())
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();
    }
}
