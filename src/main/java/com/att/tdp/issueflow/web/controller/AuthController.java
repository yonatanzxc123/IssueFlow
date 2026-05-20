package com.att.tdp.issueflow.web.controller;

import com.att.tdp.issueflow.service.AuthService;
import com.att.tdp.issueflow.web.dto.request.LoginRequest;
import com.att.tdp.issueflow.web.dto.response.AuthTokenResponse;
import com.att.tdp.issueflow.web.dto.response.CurrentUserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public AuthTokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        authService.logout(authorizationHeader);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public CurrentUserResponse me(Authentication authentication) {
        return authService.currentUser(authentication);
    }
}
