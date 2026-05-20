package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.domain.RevokedToken;
import com.att.tdp.issueflow.domain.User;
import com.att.tdp.issueflow.exception.UnauthorizedException;
import com.att.tdp.issueflow.repository.RevokedTokenRepository;
import com.att.tdp.issueflow.repository.UserRepository;
import com.att.tdp.issueflow.security.JwtService;
import com.att.tdp.issueflow.security.UserPrincipal;
import com.att.tdp.issueflow.web.dto.request.LoginRequest;
import com.att.tdp.issueflow.web.dto.response.AuthTokenResponse;
import com.att.tdp.issueflow.web.dto.response.CurrentUserResponse;
import com.att.tdp.issueflow.web.mapper.UserMapper;
import java.time.Instant;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final String BEARER_PREFIX = "Bearer ";

    private final UserRepository userRepository;
    private final RevokedTokenRepository revokedTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            RevokedTokenRepository revokedTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.revokedTokenRepository = revokedTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public AuthTokenResponse login(LoginRequest request) {
        User user = userRepository.findByUsernameIgnoreCase(request.username())
                .orElseThrow(() -> new UnauthorizedException("Bad credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Bad credentials");
        }

        return new AuthTokenResponse(jwtService.generateToken(user), "Bearer", jwtService.getExpirationSeconds());
    }

    @Transactional
    public void logout(String authorizationHeader) {
        String token = extractBearerToken(authorizationHeader);
        String tokenHash = jwtService.hashToken(token);
        Instant expiresAt = jwtService.extractExpiration(token);

        if (!revokedTokenRepository.existsByTokenHash(tokenHash)) {
            RevokedToken revokedToken = new RevokedToken();
            revokedToken.setTokenHash(tokenHash);
            revokedToken.setExpiresAt(expiresAt);
            revokedTokenRepository.save(revokedToken);
        }
        revokedTokenRepository.deleteByExpiresAtBefore(Instant.now());
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse currentUser(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new UnauthorizedException("Authenticated user no longer exists"));
        return new CurrentUserResponse(UserMapper.toResponse(user));
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new UnauthorizedException("Missing bearer token");
        }
        return authorizationHeader.substring(BEARER_PREFIX.length());
    }
}
