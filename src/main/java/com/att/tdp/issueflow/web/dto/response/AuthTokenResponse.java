package com.att.tdp.issueflow.web.dto.response;

public record AuthTokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}
