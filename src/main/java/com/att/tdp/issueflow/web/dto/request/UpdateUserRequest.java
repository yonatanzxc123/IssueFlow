package com.att.tdp.issueflow.web.dto.request;

import com.att.tdp.issueflow.domain.enums.Role;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Size(max = 255) String fullName,
        Role role
) {
}
