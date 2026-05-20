package com.att.tdp.issueflow.web.dto.response;

import com.att.tdp.issueflow.domain.enums.Role;

public record UserResponse(
        Long id,
        String username,
        String email,
        String fullName,
        Role role
) {
}
