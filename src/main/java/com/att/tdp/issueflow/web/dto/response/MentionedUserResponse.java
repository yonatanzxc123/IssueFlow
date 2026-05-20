package com.att.tdp.issueflow.web.dto.response;

public record MentionedUserResponse(
        Long id,
        String username,
        String fullName
) {
}
